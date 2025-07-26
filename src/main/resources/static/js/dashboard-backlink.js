// 백링크 체크 관련 함수들

// 백링크 전체 데이터 저장용 변수
let allBacklinkData = [];
let backlinkChart = null; // 차트 인스턴스 저장
let lastBacklinkSummaryData = null; // 마지막 요약 데이터 저장

// 백링크 탭 초기화
function initBacklinkTab() {
  // 날짜 필드 초기화 (오늘부터 7일 전까지)
  const now = new Date();
  const endDate = new Date(now);
  const startDate = new Date(now);
  startDate.setDate(startDate.getDate() - 6); // 7일 전
  
  const formatDate = (date) => {
    return date.getFullYear() + '-' +
        String(date.getMonth() + 1).padStart(2, '0') + '-' +
        String(date.getDate()).padStart(2, '0');
  };
  
  document.getElementById('backlink-start-date').value = formatDate(startDate);
  document.getElementById('backlink-end-date').value = formatDate(endDate);
}

// 백링크 날짜 유효성 검증
function validateBacklinkDates() {
  const startDate = document.getElementById('backlink-start-date').value;
  const endDate = document.getElementById('backlink-end-date').value;
  
  if (startDate && endDate && startDate > endDate) {
    const message = typeof t === 'function' ? t('messages.invalidDateRange') : '시작일이 종료일보다 늦을 수 없어요!';
    alert(message);
    document.getElementById('backlink-end-date').value = startDate;
  }
}

// 백링크 빠른 날짜 설정
function setBacklinkDateRange(days) {
  const excludeToday = document.getElementById('backlink-exclude-today').checked;
  const now = new Date();
  const endDate = new Date(now);
  
  if (excludeToday && days > 1) {
    endDate.setDate(endDate.getDate() - 1); // 어제를 종료일로
  }
  
  const startDate = new Date(endDate);
  startDate.setDate(startDate.getDate() - (days - 1));
  
  const formatDate = (date) => {
    return date.getFullYear() + '-' +
        String(date.getMonth() + 1).padStart(2, '0') + '-' +
        String(date.getDate()).padStart(2, '0');
  };
  
  document.getElementById('backlink-start-date').value = formatDate(startDate);
  document.getElementById('backlink-end-date').value = formatDate(endDate);
}

// 백링크 데이터 가져오기
async function fetchBacklinkData() {
  const startDate = document.getElementById('backlink-start-date').value;
  const endDate = document.getElementById('backlink-end-date').value;
  
  if (!startDate || !endDate) {
    const message = typeof t === 'function' ? t('messages.selectDates') : '시작일과 종료일을 모두 선택해주세요!';
    alert(message);
    return;
  }
  
  // UI 상태 변경
  document.getElementById('backlink-initial-message').style.display = 'none';
  document.getElementById('backlink-loading').style.display = 'block';
  document.getElementById('backlink-data-container').style.display = 'none';
  
  try {
    const response = await fetch(`/api/referral-traffic?startDate=${startDate}&endDate=${endDate}`);
    if (!response.ok) {
      throw new Error('Failed to fetch backlink data');
    }
    
    const data = await response.json();
    // 전체 데이터 저장
    allBacklinkData = data;
    // 도메인 목록 추출 및 드롭다운 업데이트
    updateDomainFilter(data);
    // 상위 10개 누적합 표시
    renderBacklinkSummary(data);
    // 데이터 렌더링
    renderBacklinkData(data);
    
    // 업데이트 시간 갱신
    updateBacklinkTime();
    
    // UI 상태 변경
    document.getElementById('backlink-loading').style.display = 'none';
    document.getElementById('backlink-data-container').style.display = 'block';
  } catch (error) {
    console.error('Error fetching backlink data:', error);
    alert('백링크 데이터를 가져오는 중 오류가 발생했습니다.');
    document.getElementById('backlink-loading').style.display = 'none';
    document.getElementById('backlink-initial-message').style.display = 'block';
  }
}

// 백링크 데이터 렌더링
function renderBacklinkData(data) {
  const tbody = document.getElementById('backlink-table');
  tbody.innerHTML = ''; // 기존 데이터 초기화
  
  if (!data || data.length === 0) {
    const noDataMessage = typeof t === 'function' ? t('messages.noData') : '데이터가 없거나 아직 카테고리가 동기화되지 않았어요!';
    tbody.innerHTML = `
      <tr>
        <td colspan="5" style="text-align: center; padding: 40px; color: #666;">
          ${noDataMessage}
        </td>
      </tr>
    `;
    return;
  }
  
  // 전체 세션수 계산
  const totalSessions = data.reduce((sum, item) => sum + item.sessions, 0);
  
  // 데이터 렌더링
  data.forEach((item, index) => {
    const ratio = totalSessions > 0 ? (item.sessions / totalSessions * 100).toFixed(1) : 0;
    const row = document.createElement('tr');
    row.innerHTML = `
      <td>${index + 1}</td>
      <td>${item.sourceSite || '(direct)'}</td>
      <td>${item.landingPage}</td>
      <td>${item.sessions.toLocaleString()}</td>
      <td>${ratio}%</td>
    `;
    tbody.appendChild(row);
  });
}

// 백링크 업데이트 시간 갱신
function updateBacklinkTime() {
  const now = new Date();
  
  // 현재 언어에 맞는 로케일 설정
  const locale = window.getCurrentLanguage ? 
    (window.getCurrentLanguage() === 'ko' ? 'ko-KR' : 
     window.getCurrentLanguage() === 'en' ? 'en-US' : 'zh-CN') : 'ko-KR';
  
  // 로케일에 맞는 시간 형식
  const timeString = now.toLocaleString(locale, {
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
    hour: 'numeric',
    minute: 'numeric',
    second: 'numeric'
  });
  
  const lastUpdateText = typeof t === 'function' ? t('labels.lastUpdate') : '마지막 업데이트';
  const timeElement = document.getElementById('backlink-update-time');
  if (timeElement) {
    timeElement.innerHTML = `<span data-i18n="labels.lastUpdate">${lastUpdateText}</span>: ${timeString}`;
  }
}

// 도메인 필터 드롭다운 업데이트
function updateDomainFilter(data) {
  const domainFilter = document.getElementById('backlink-domain-filter');
  
  // 기존 옵션 초기화 (전체 도메인 옵션은 유지)
  domainFilter.innerHTML = `<option value="">${typeof t === 'function' ? t('filters.allDomains') : '전체 도메인'}</option>`;
  
  // 도메인별 카운트 계산
  const domainCounts = {};
  data.forEach(item => {
    if (!domainCounts[item.sourceSite]) {
      domainCounts[item.sourceSite] = 0;
    }
    domainCounts[item.sourceSite]++;
  });
  
  // 도메인 목록을 카운트 기준으로 내림차순 정렬
  const sortedDomains = Object.entries(domainCounts)
    .sort((a, b) => b[1] - a[1])  // 카운트가 많은 순으로 정렬
    .map(([domain, count]) => ({ domain, count }));
  
  // 드롭다운에 옵션 추가
  sortedDomains.forEach(({ domain, count }) => {
    const option = document.createElement('option');
    option.value = domain;
    option.textContent = `${domain} (${count})`;
    domainFilter.appendChild(option);
  });
}

// 도메인별 필터링
function filterBacklinkByDomain() {
  const selectedDomain = document.getElementById('backlink-domain-filter').value;
  
  // 상위 10개 테이블에서 하이라이트 처리
  highlightSelectedDomain(selectedDomain);
  
  // 차트 다시 그리기 (선택된 도메인 강조)
  if (lastBacklinkSummaryData) {
    drawBacklinkChart(lastBacklinkSummaryData.sortedDomains, lastBacklinkSummaryData.totalSessions);
  }
  
  if (!selectedDomain) {
    // 전체 도메인 선택시 전체 데이터 표시
    renderBacklinkData(allBacklinkData);
  } else {
    // 선택한 도메인만 필터링 (상위 10개는 영향 없음)
    const filteredData = allBacklinkData.filter(item => item.sourceSite === selectedDomain);
    renderBacklinkData(filteredData);
  }
}

// 선택한 도메인 하이라이트
function highlightSelectedDomain(selectedDomain) {
  const summaryRows = document.querySelectorAll('#backlink-summary-tbody tr');
  
  summaryRows.forEach(row => {
    // 트랜지션 효과 추가
    row.style.transition = 'all 0.3s ease';
    
    const domainCell = row.querySelector('td:nth-child(2)');
    if (domainCell) {
      if (selectedDomain && domainCell.textContent === selectedDomain) {
        row.style.background = 'rgba(147, 51, 234, 0.15)';
        row.style.borderLeft = '3px solid #9333ea';
        row.style.transform = 'translateX(3px)';
      } else {
        row.style.background = '';
        row.style.borderLeft = '';
        row.style.transform = '';
      }
    }
  });
}

// 백링크 상위 10개 누적합 렌더링
function renderBacklinkSummary(data) {
  const tbody = document.getElementById('backlink-summary-tbody');
  
  // 데이터가 없으면 처리
  if (!data || data.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="4" style="text-align: center; padding: 20px; color: #666;">
          ${typeof t === 'function' ? t('messages.noData') : '데이터가 없습니다.'}
        </td>
      </tr>
    `;
    // 차트도 초기화
    if (backlinkChart) {
      backlinkChart.destroy();
      backlinkChart = null;
    }
    lastBacklinkSummaryData = null; // 데이터 초기화
    return;
  }
  
  // 도메인별로 세션수 합산
  const domainSessions = {};
  data.forEach(item => {
    if (!domainSessions[item.sourceSite]) {
      domainSessions[item.sourceSite] = 0;
    }
    domainSessions[item.sourceSite] += item.sessions;
  });
  
  // 배열로 변환하고 정렬
  const sortedDomains = Object.entries(domainSessions)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 10); // 상위 10개만
  
  const totalSessions = sortedDomains.reduce((sum, [_, sessions]) => sum + sessions, 0);
  
  // 마지막 데이터 저장 (테마 변경시 사용)
  lastBacklinkSummaryData = { sortedDomains, totalSessions };
  
  // 테이블에 데이터 표시
  tbody.innerHTML = '';
  
  sortedDomains.forEach(([domain, sessions], index) => {
    const ratio = totalSessions > 0 ? (sessions / totalSessions * 100).toFixed(1) : 0;
    const row = document.createElement('tr');
    row.innerHTML = `
      <td>${index + 1}</td>
      <td style="max-width: 200px; overflow-x: auto; white-space: nowrap;">${domain}</td>
      <td>${sessions.toLocaleString()}</td>
      <td>${ratio}%</td>
    `;
    tbody.appendChild(row);
  });
  
  // 차트 그리기
  drawBacklinkChart(sortedDomains, totalSessions);
  
  // 현재 선택된 도메인이 있다면 하이라이트 유지
  const selectedDomain = document.getElementById('backlink-domain-filter')?.value;
  if (selectedDomain) {
    highlightSelectedDomain(selectedDomain);
  }
}

// 테마 변경시 백링크 차트 업데이트
function updateBacklinkChartTheme() {
  // 백링크 탭이 활성화되어 있고 데이터가 있을 때만 업데이트
  if (lastBacklinkSummaryData && document.getElementById('backlink-check-content').classList.contains('active')) {
    drawBacklinkChart(lastBacklinkSummaryData.sortedDomains, lastBacklinkSummaryData.totalSessions);
  }
}

// 백링크 차트 그리기
function drawBacklinkChart(sortedDomains, totalSessions) {
  const ctx = document.getElementById('backlink-chart');
  if (!ctx) return; // 캔버스가 없으면 리턴
  
  const context = ctx.getContext('2d');
  
  // 기존 차트가 있으면 제거
  if (backlinkChart) {
    backlinkChart.destroy();
  }
  
  // 현재 선택된 도메인
  const selectedDomain = document.getElementById('backlink-domain-filter')?.value;
  
  const labels = sortedDomains.map(([domain, _]) => {
    // 긴 도메인은 축약
    return domain.length > 20 ? domain.substring(0, 17) + '...' : domain;
  });
  
  const data = sortedDomains.map(([_, sessions]) => sessions);
  const percentages = sortedDomains.map(([_, sessions]) => 
    (sessions / totalSessions * 100).toFixed(1)
  );
  
  // 색상 팔레트 (보라색 계열)
  const baseColors = [
    'rgba(147, 51, 234, 0.8)',   // 보라색
    'rgba(168, 85, 247, 0.8)',   // 연보라
    'rgba(196, 110, 255, 0.8)',  
    'rgba(217, 138, 255, 0.8)',  
    'rgba(232, 169, 255, 0.8)',  
    'rgba(245, 200, 255, 0.8)',  
    'rgba(139, 92, 246, 0.8)',   // 파란보라
    'rgba(124, 58, 237, 0.8)',   
    'rgba(109, 40, 217, 0.8)',   
    'rgba(91, 33, 182, 0.8)'     // 진한 보라
  ];
  
  // 선택된 도메인의 인덱스 찾기
  const selectedIndex = selectedDomain ? 
    sortedDomains.findIndex(([domain, _]) => domain === selectedDomain) : -1;
  
  // 선택된 도메인은 더 진한 색으로
  const colors = baseColors.map((color, index) => {
    if (index === selectedIndex) {
      return color.replace('0.8', '1'); // 불투명도 증가
    }
    return color;
  });
  
  // 차트 데이터에서 offset 설정 (선택된 것만 약간 분리)
  const offsets = sortedDomains.map((_, index) => 
    index === selectedIndex ? 10 : 0
  );
  
  backlinkChart = new Chart(context, {
    type: 'doughnut',
    data: {
      labels: labels,
      datasets: [{
        data: data,
        backgroundColor: colors,
        borderColor: colors.map(color => color.replace('0.8', '1')),
        borderWidth: 2,
        offset: offsets // 선택된 부분만 분리
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      // 클릭 이벤트 비활성화
      events: ['mousemove', 'mouseout', 'touchstart', 'touchmove'],
      plugins: {
        legend: {
          position: 'right',
          labels: {
            // 테마에 따른 텍스트 색상 설정
            color: function() {
              return document.body.classList.contains('light-theme') ? '#374151' : '#e4e4e7';
            },
            font: {
              size: 11,
              family: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif'
            },
            padding: 10,
            generateLabels: function(chart) {
              const data = chart.data;
              if (data.labels.length && data.datasets.length) {
                return data.labels.map((label, i) => {
                  const dataset = data.datasets[0];
                  const value = dataset.data[i];
                  const percentage = percentages[i];
                  
                  return {
                    text: `${label} (${percentage}%)`,
                    fillStyle: dataset.backgroundColor[i],
                    strokeStyle: dataset.borderColor[i],
                    lineWidth: dataset.borderWidth,
                    hidden: false,
                    index: i,
                    // 텍스트 색상 명시적 설정
                    fontColor: document.body.classList.contains('light-theme') ? '#374151' : '#e4e4e7'
                  };
                });
              }
              return [];
            },
            usePointStyle: true,
            pointStyle: 'circle'
          },
          // 클릭 이벤트 비활성화
          onClick: null
        },
        tooltip: {
          callbacks: {
            label: function(context) {
              const label = context.label || '';
              const value = context.parsed || 0;
              const percentage = percentages[context.dataIndex];
              return `${label}: ${value.toLocaleString()} (${percentage}%)`;
            }
          }
        }
      }
    }
  });
}