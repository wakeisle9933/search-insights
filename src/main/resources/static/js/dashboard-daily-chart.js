// 일간 차트 관련 함수들

let dailyChart = null;
let dailyChartData = {
  dates: [],
  activeUsers: [],
  pageViews: []
};

// 날짜 유효성 검사
function validateChartDates() {
  const startDate = document.getElementById('chart-start-date').value;
  const endDate = document.getElementById('chart-end-date').value;

  if (startDate && endDate && new Date(startDate) > new Date(endDate)) {
    alert('🚨 ' + (window.t ? window.t('errors.dateRangeInvalid') : '시작일이 종료일보다 늦을 수 없어요!'));
    document.getElementById('chart-end-date').value = startDate;
  }
}

// 차트 날짜 범위 설정 (빠른 선택)
function setChartDateRange(days, autoFetch = true) {
  const now = new Date();
  const excludeToday = document.getElementById('chart-exclude-today').checked;
  
  let endDate = new Date(now);
  let startDate = new Date(now);
  
  if (excludeToday) {
    // 어제를 종료일로 설정
    endDate.setDate(endDate.getDate() - 1);
    // 시작일은 종료일에서 days만큼 뺀 날짜
    startDate = new Date(endDate);
    startDate.setDate(startDate.getDate() - days + 1);
  } else {
    // 오늘을 종료일로 설정
    // 시작일은 오늘에서 days만큼 뺀 날짜
    startDate.setDate(startDate.getDate() - days + 1);
  }
  
  // 날짜 형식 변환 (YYYY-MM-DD)
  const formatDate = (date) => {
    return date.getFullYear() + '-' +
        String(date.getMonth() + 1).padStart(2, '0') + '-' +
        String(date.getDate()).padStart(2, '0');
  };
  
  document.getElementById('chart-start-date').value = formatDate(startDate);
  document.getElementById('chart-end-date').value = formatDate(endDate);
  
  // autoFetch가 true일 때만 자동 조회
  if (autoFetch) {
    fetchDailyChartData();
  }
}

// 일간 데이터 가져오기
async function fetchDailyChartData() {
  const startDate = document.getElementById('chart-start-date').value;
  const endDate = document.getElementById('chart-end-date').value;
  
  if (!startDate || !endDate) {
    alert('❓ ' + (window.t ? window.t('errors.selectAllDates') : '시작일과 종료일을 모두 선택해주세요!'));
    return;
  }
  
  const loading = document.getElementById('chart-loading');
  const canvas = document.getElementById('daily-chart-canvas');
  const initialMessage = document.getElementById('chart-initial-message');
  
  // 로딩 표시
  loading.style.display = 'block';
  canvas.style.display = 'none';
  initialMessage.style.display = 'none';
  
  // 초기화
  dailyChartData = {
    dates: [],
    activeUsers: [],
    pageViews: []
  };
  
  try {
    // 날짜 범위 계산
    const start = new Date(startDate);
    const end = new Date(endDate);
    const promises = [];
    
    // 시작일부터 종료일까지 루프
    const currentDate = new Date(start);
    while (currentDate <= end) {
      // 현재 날짜를 복사해서 사용 (클로저 문제 방지)
      const thisDate = new Date(currentDate);
      const dateStr = formatDateForAPI(thisDate);
      const displayDate = formatDateForDisplay(thisDate);
      
      promises.push(
        fetch(`/api/custom-date-pageviews?startDate=${dateStr}&endDate=${dateStr}`)
          .then(response => response.json())
          .then(data => ({
            date: dateStr,
            displayDate: displayDate,
            activeUsers: data.activeUsers || 0,
            pageViews: data.pageViews ? data.pageViews.reduce((sum, item) => sum + item.pageViews, 0) : 0
          }))
      );
      
      // 다음 날짜로 이동
      currentDate.setDate(currentDate.getDate() + 1);
    }
    
    // 모든 데이터를 병렬로 가져오기
    const results = await Promise.all(promises);
    
    // 데이터 정리
    results.forEach(result => {
      dailyChartData.dates.push(result.displayDate);
      dailyChartData.activeUsers.push(result.activeUsers);
      dailyChartData.pageViews.push(Math.round(result.pageViews));
    });
    
    // window 객체에도 데이터 저장
    window.dailyChartData = dailyChartData;
    
    // 차트 그리기
    renderDailyChart();
    
    // 업데이트 시간 표시
    updateTime('daily-chart-update-time');
    
  } catch (error) {
    alert('차트 데이터를 불러오는데 실패했습니다.');
  } finally {
    // 로딩 숨기고 차트 표시
    loading.style.display = 'none';
    canvas.style.display = 'block';
    
    // 초기 메시지도 숨기기
    const initialMessage = document.getElementById('chart-initial-message');
    if (initialMessage) {
      initialMessage.style.display = 'none';
    }
  }
}

// 차트 렌더링
function renderDailyChart() {
  const canvas = document.getElementById('daily-chart-canvas');
  if (!canvas) {
    return;
  }
  
  // 캔버스가 숨겨져 있으면 표시
  canvas.style.display = 'block';
  
  const ctx = canvas.getContext('2d');
  
  // 기존 차트가 있으면 제거
  if (dailyChart) {
    dailyChart.destroy();
  }
  
  // window 객체에도 업데이트
  window.dailyChart = null;
  
  // 테마에 따른 색상 설정
  const isDarkTheme = !document.body.classList.contains('light-theme');
  const textColor = isDarkTheme ? '#e0e0e0' : '#333';
  const gridColor = isDarkTheme ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.1)';
  
  // 새 차트 생성
  dailyChart = new Chart(ctx, {
    type: 'bar',
    data: {
      labels: dailyChartData.dates,
      datasets: [
        {
          label: window.t ? window.t('chartLabels.activeUsers') : '활성 사용자',
          data: dailyChartData.activeUsers,
          backgroundColor: 'rgba(54, 162, 235, 0.5)',
          borderColor: 'rgba(54, 162, 235, 1)',
          borderWidth: 1,
          yAxisID: 'y-users',
          order: 2
        },
        {
          label: window.t ? window.t('chartLabels.totalPageviews') : '전체 조회수',
          data: dailyChartData.pageViews,
          type: 'line',
          borderColor: 'rgba(255, 99, 132, 1)',
          backgroundColor: 'rgba(255, 99, 132, 0.1)',
          borderWidth: 2,
          fill: true,
          tension: 0.4,
          yAxisID: 'y-views',
          order: 1
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: {
        mode: 'index',
        intersect: false
      },
      onClick: async (event, elements) => {
        if (elements.length > 0) {
          const index = elements[0].index;
          const dateStr = dailyChartData.dates[index];
          const activeUsers = dailyChartData.activeUsers[index];
          const pageViews = dailyChartData.pageViews[index];
          
          // 날짜 형식 변환 (예: "12/25" -> "2023-12-25")
          const currentYear = new Date().getFullYear();
          const [month, day] = dateStr.split('/');
          const fullDate = `${currentYear}-${month.padStart(2, '0')}-${day.padStart(2, '0')}`;
          
          // 상세 데이터 표시
          showDailyChartDetail(fullDate, dateStr, activeUsers, pageViews);
        }
      },
      plugins: {
        title: {
          display: false
        },
        legend: {
          position: 'top',
          labels: {
            color: textColor,
            usePointStyle: true,
            padding: 20
          }
        },
        tooltip: {
          mode: 'index',
          intersect: false,
          callbacks: {
            label: function(context) {
              let label = context.dataset.label || '';
              if (label) {
                label += ': ';
              }
              if (context.parsed.y !== null) {
                label += new Intl.NumberFormat('ko-KR').format(context.parsed.y);
              }
              return label;
            }
          }
        }
      },
      scales: {
        x: {
          ticks: {
            color: textColor
          },
          grid: {
            color: gridColor
          }
        },
        'y-users': {
          type: 'linear',
          display: true,
          position: 'left',
          title: {
            display: true,
            text: window.t ? window.t('chartLabels.activeUsers') : '활성 사용자',
            color: textColor
          },
          ticks: {
            color: 'rgba(54, 162, 235, 1)',
            callback: function(value) {
              return new Intl.NumberFormat('ko-KR').format(value);
            }
          },
          grid: {
            color: gridColor
          }
        },
        'y-views': {
          type: 'linear',
          display: true,
          position: 'right',
          title: {
            display: true,
            text: window.t ? window.t('chartLabels.totalPageviews') : '전체 조회수',
            color: textColor
          },
          ticks: {
            color: 'rgba(255, 99, 132, 1)',
            callback: function(value) {
              return new Intl.NumberFormat('ko-KR').format(value);
            }
          },
          grid: {
            drawOnChartArea: false
          }
        }
      }
    }
  });
  
  // window 객체에 차트 인스턴스 저장
  window.dailyChart = dailyChart;
}

// 날짜 형식 변환 함수들
function formatDateForAPI(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function formatDateForDisplay(date) {
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${month}/${day}`;
}

// 일간 차트 탭 초기화
function initDailyChartTab() {
  // 날짜 필드 초기화 (최근 7일)
  const now = new Date();
  const startDate = new Date(now);
  startDate.setDate(startDate.getDate() - 6); // 7일 전 (오늘 포함)
  
  const formatDate = (date) => {
    return date.getFullYear() + '-' +
        String(date.getMonth() + 1).padStart(2, '0') + '-' +
        String(date.getDate()).padStart(2, '0');
  };
  
  // 날짜 필드가 비어있을 때만 초기값 설정
  if (!document.getElementById('chart-start-date').value) {
    document.getElementById('chart-start-date').value = formatDate(startDate);
    document.getElementById('chart-end-date').value = formatDate(now);
  }
}

// 테마 변경 시 차트 다시 그리기
function handleThemeChange() {
  // 차트가 존재하고, 일간 차트 탭이 활성화되어 있고, 데이터가 있을 때만 다시 그리기
  if (dailyChart && 
      document.getElementById('daily-chart-content').classList.contains('active') && 
      dailyChartData.dates.length > 0) {
    renderDailyChart();
  }
}

// toggleTheme 함수 오버라이드
const originalToggleTheme = window.toggleTheme;
window.toggleTheme = function() {
  originalToggleTheme();
  handleThemeChange();
};

// 일간 차트 상세 데이터를 위한 전역 변수
let dailyDetailData = null;
let currentDailyDetailDate = null;

// 날짜별 상세 데이터 표시 함수
async function showDailyChartDetail(fullDate, displayDate, activeUsers, pageViews) {
  const detailBox = document.getElementById('daily-chart-detail');
  const detailTitle = document.getElementById('daily-chart-detail-title');
  const detailActiveUsers = document.getElementById('daily-detail-active-users');
  const detailPageviews = document.getElementById('daily-detail-total-pageviews');
  const loadingDiv = document.getElementById('daily-detail-loading');
  
  // 날짜 저장
  currentDailyDetailDate = fullDate;
  
  // 제목 설정
  const detailText = window.t ? window.t('ui.dailyChartDetail') : '상세 분석';
  detailTitle.textContent = `📈 ${displayDate} ${detailText}`;
  
  // 제목 업데이트
  const fullTitle = document.getElementById('daily-detail-full-title');
  if (fullTitle) {
    fullTitle.textContent = `📈 페이지 제목별 조회수 (${displayDate})`;
  }
  
  // 요약 정보 표시
  detailActiveUsers.textContent = new Intl.NumberFormat('ko-KR').format(activeUsers);
  detailPageviews.textContent = new Intl.NumberFormat('ko-KR').format(pageViews);
  
  // 상세 영역 먼저 표시
  detailBox.style.display = 'block';
  
  // 서브 탭 초기화 - 전체 제목 탭 활성화
  const allSubTabs = detailBox.querySelectorAll('.sub-tab');
  const allSubContents = detailBox.querySelectorAll('.sub-tab-content');
  allSubTabs.forEach(tab => tab.classList.remove('active'));
  allSubContents.forEach(content => content.classList.remove('active'));
  
  if (allSubTabs[0]) allSubTabs[0].classList.add('active');
  const fullContent = document.getElementById('daily-detail-full-content');
  if (fullContent) fullContent.classList.add('active');
  
  // 카테고리 필터 드롭다운 표시
  const fullCategoryFilter = document.getElementById('daily-detail-full-category-filter');
  if (fullCategoryFilter) {
    fullCategoryFilter.classList.add('visible');
  }
  
  // 로딩 표시
  loadingDiv.style.display = 'block';
  
  // 스크롤 이동 (부드럽게)
  setTimeout(() => {
    detailBox.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }, 100);
  
  // 상세 데이터 가져오기
  try {
    const response = await fetch(`/api/custom-date-pageviews?startDate=${fullDate}&endDate=${fullDate}`);
    const data = await response.json();
    
    // 데이터 저장
    dailyDetailData = data;
    
    // 카테고리 드롭다운 초기화 - wpCategoryData가 있을 때만
    if (window.wpCategoryData && Object.keys(window.wpCategoryData.categories || {}).length > 0) {
      initializeCategoryDropdown('daily-detail');
      checkCategoryDataAvailability('daily-detail');
    } else {
      // 카테고리 데이터가 없으면 나중에 로드되면 초기화하도록 설정
      const checkInterval = setInterval(() => {
        if (window.wpCategoryData && Object.keys(window.wpCategoryData.categories || {}).length > 0) {
          initializeCategoryDropdown('daily-detail');
          checkCategoryDataAvailability('daily-detail');
          clearInterval(checkInterval);
        }
      }, 500);
      
      // 10초 후에는 자동으로 중지
      setTimeout(() => clearInterval(checkInterval), 10000);
    }
    
    // 전체 제목 테이블 업데이트
    if (data.pageViews && data.pageViews.length > 0) {
      updatePageViewsTable(data.pageViews, 'daily-detail-page-title-views');
    } else {
      const noDataText = window.t ? window.t('messages.noData') : '데이터가 없습니다';
      document.getElementById('daily-detail-page-title-views').innerHTML = 
        `<tr><td colspan="4" style="text-align: center; color: #999;">${noDataText}</td></tr>`;
    }
    
    // 접두어 데이터 업데이트
    if (data.pageViews) {
      updatePrefixViewsTable(data.pageViews, 'daily-detail-prefix1-views', 1);
      updatePrefixViewsTable(data.pageViews, 'daily-detail-prefix2-views', 2);
      updatePrefixViewsTable(data.pageViews, 'daily-detail-prefix3-views', 3);
    }
    
    // 카테고리별 데이터 업데이트
    if (data.pageViews && window.wpCategoryData) {
      updateCategoryTableForDaily(data.pageViews, 'daily-detail-category-views');
    }
    
    // 로딩 숨기기
    loadingDiv.style.display = 'none';
    
  } catch (error) {
    console.error('상세 데이터 로드 실패:', error);
    
    // 로딩 숨기기
    loadingDiv.style.display = 'none';
    
    // 에러 메시지 표시
    const errorText = window.t ? window.t('errors.loadDetailFailed') : '상세 데이터를 불러오는데 실패했습니다.';
    document.getElementById('daily-detail-page-title-views').innerHTML = 
      `<tr><td colspan="4" style="text-align: center; color: #ff6b6b;">${errorText}</td></tr>`;
  }
}


// 상세 데이터 닫기 함수
function closeDailyChartDetail() {
  document.getElementById('daily-chart-detail').style.display = 'none';
  
  // 데이터 초기화
  dailyDetailData = null;
  currentDailyDetailDate = null;
}

// 일간 차트 상세의 서브 탭 전환 처리
function handleDailyDetailSubTab(tabType) {
  const detailBox = document.getElementById('daily-chart-detail');
  const allSubTabs = detailBox.querySelectorAll('.sub-tab');
  const allSubContents = detailBox.querySelectorAll('.sub-tab-content');
  
  // 모든 탭과 컨텐츠 비활성화
  allSubTabs.forEach(tab => tab.classList.remove('active'));
  allSubContents.forEach(content => content.classList.remove('active'));
  
  // 클릭한 탭 활성화
  const tabIndex = Array.from(allSubTabs).findIndex(tab => 
    tab.onclick && tab.onclick.toString().includes(tabType)
  );
  if (tabIndex !== -1) {
    allSubTabs[tabIndex].classList.add('active');
  }
  
  // 해당 컨텐츠 표시
  const contentId = `daily-detail-${tabType}-content`;
  const content = document.getElementById(contentId);
  if (content) {
    content.classList.add('active');
  }
}

// 일간 차트 상세의 카테고리 필터링
function filterDailyDetailByCategory(categoryId) {
  if (!dailyDetailData || !dailyDetailData.pageViews) return;
  
  let filteredData = dailyDetailData.pageViews;
  
  if (categoryId && window.wpCategoryData) {
    filteredData = dailyDetailData.pageViews.filter(page => {
      const postId = extractPostId(page.pagePath);
      return postId && window.wpCategoryData.posts[postId] && 
             window.wpCategoryData.posts[postId].includes(parseInt(categoryId));
    });
  }
  
  updatePageViewsTable(filteredData, 'daily-detail-page-title-views');
}

// 카테고리 드롭다운 초기화
function initializeCategoryDropdown(mainTab) {
  const dropdown = document.getElementById(`${mainTab}-full-category-select`);
  if (!dropdown || !window.wpCategoryData) return;
  
  // 기존 옵션 제거 (전체 카테고리 제외)
  while (dropdown.options.length > 1) {
    dropdown.remove(1);
  }
  
  // 카테고리 옵션 추가
  Object.entries(window.wpCategoryData.categories).forEach(([id, name]) => {
    const option = document.createElement('option');
    option.value = id;
    option.textContent = name.replace(/&amp;/g, '&');
    dropdown.appendChild(option);
  });
}

// 카테고리 데이터 사용 가능 여부 확인
function checkCategoryDataAvailability(mainTab) {
  const syncBanner = document.getElementById(`sync-banner-${mainTab}`);
  if (!syncBanner) return;
  
  if (window.wpCategoryData && Object.keys(window.wpCategoryData.categories).length > 0) {
    syncBanner.style.display = 'none';
  } else {
    syncBanner.style.display = 'block';
  }
}

// Post ID 추출 함수
function extractPostId(pagePath) {
  if (!pagePath) return null;
  const match = pagePath.match(/\/(\d+)(?:\/|$)/);
  return match ? match[1] : null;
}

// 카테고리별 테이블 업데이트 (일간 차트용)
function updateCategoryTableForDaily(pageViews, tableId) {
  if (!window.wpCategoryData || !pageViews) return;
  
  const tableBody = document.getElementById(tableId);
  if (!tableBody) return;
  
  // 카테고리별 조회수 집계
  const categoryViews = {};
  
  pageViews.forEach(page => {
    const postId = extractPostId(page.pagePath);
    if (postId && window.wpCategoryData.posts[postId]) {
      window.wpCategoryData.posts[postId].forEach(catId => {
        if (!categoryViews[catId]) {
          categoryViews[catId] = 0;
        }
        categoryViews[catId] += page.pageViews;
      });
    }
  });
  
  // 배열로 변환하고 정렬
  const sortedCategories = Object.entries(categoryViews)
    .map(([catId, views]) => ({
      id: catId,
      name: window.wpCategoryData.categories[catId] || '알 수 없음',
      views: views
    }))
    .sort((a, b) => b.views - a.views);
  
  // 총 조회수 계산
  const totalViews = sortedCategories.reduce((sum, cat) => sum + cat.views, 0);
  
  // 테이블 업데이트
  tableBody.innerHTML = '';
  
  sortedCategories.forEach((cat, index) => {
    const row = document.createElement('tr');
    
    // 순번
    const numCell = document.createElement('td');
    numCell.textContent = index + 1;
    row.appendChild(numCell);
    
    // 카테고리명
    const nameCell = document.createElement('td');
    nameCell.textContent = cat.name.replace(/&amp;/g, '&');
    row.appendChild(nameCell);
    
    // 조회수
    const viewsCell = document.createElement('td');
    viewsCell.textContent = Math.round(cat.views);
    row.appendChild(viewsCell);
    
    // 비율 (프로그레스 바)
    const ratioCell = document.createElement('td');
    const percentage = totalViews > 0 ? (cat.views / totalViews * 100) : 0;
    
    const progressContainer = document.createElement('div');
    progressContainer.style.display = 'flex';
    progressContainer.style.alignItems = 'center';
    
    const progressBar = document.createElement('div');
    progressBar.className = 'progress-bar';
    progressBar.style.width = '100px';
    
    const progressValue = document.createElement('div');
    progressValue.className = 'progress-value';
    progressValue.style.width = `${percentage}%`;
    
    const percentText = document.createElement('span');
    percentText.textContent = `${percentage.toFixed(1)}%`;
    percentText.style.marginLeft = '10px';
    percentText.style.fontSize = '0.9em';
    
    progressBar.appendChild(progressValue);
    progressContainer.appendChild(progressBar);
    progressContainer.appendChild(percentText);
    ratioCell.appendChild(progressContainer);
    
    row.appendChild(ratioCell);
    tableBody.appendChild(row);
  });
  
  if (sortedCategories.length === 0) {
    const noDataText = window.t ? window.t('messages.noData') : '데이터가 없습니다';
    tableBody.innerHTML = `<tr><td colspan="4" style="text-align: center; color: #999;">${noDataText}</td></tr>`;
  }
}

// 카테고리별 테이블 업데이트
function updateCategoryTable(pageViews, tableId, categoryData) {
  if (!categoryData || !pageViews) return;
  
  const tableBody = document.getElementById(tableId);
  if (!tableBody) return;
  
  // 카테고리별 조회수 집계
  const categoryViews = {};
  
  pageViews.forEach(page => {
    const postId = extractPostId(page.pagePath);
    if (postId && categoryData.posts[postId]) {
      categoryData.posts[postId].forEach(catId => {
        if (!categoryViews[catId]) {
          categoryViews[catId] = 0;
        }
        categoryViews[catId] += page.pageViews;
      });
    }
  });
  
  // 배열로 변환하고 정렬
  const sortedCategories = Object.entries(categoryViews)
    .map(([catId, views]) => ({
      id: catId,
      name: categoryData.categories[catId] || '알 수 없음',
      views: views
    }))
    .sort((a, b) => b.views - a.views);
  
  // 총 조회수 계산
  const totalViews = sortedCategories.reduce((sum, cat) => sum + cat.views, 0);
  
  // 테이블 업데이트
  tableBody.innerHTML = '';
  
  sortedCategories.forEach((cat, index) => {
    const row = document.createElement('tr');
    
    // 순번
    const numCell = document.createElement('td');
    numCell.textContent = index + 1;
    row.appendChild(numCell);
    
    // 카테고리명
    const nameCell = document.createElement('td');
    nameCell.textContent = cat.name.replace(/&amp;/g, '&');
    row.appendChild(nameCell);
    
    // 조회수
    const viewsCell = document.createElement('td');
    viewsCell.textContent = Math.round(cat.views);
    row.appendChild(viewsCell);
    
    // 비율 (프로그레스 바)
    const ratioCell = document.createElement('td');
    const percentage = totalViews > 0 ? (cat.views / totalViews * 100) : 0;
    
    const progressContainer = document.createElement('div');
    progressContainer.style.display = 'flex';
    progressContainer.style.alignItems = 'center';
    
    const progressBar = document.createElement('div');
    progressBar.className = 'progress-bar';
    progressBar.style.width = '100px';
    
    const progressValue = document.createElement('div');
    progressValue.className = 'progress-value';
    progressValue.style.width = `${percentage}%`;
    
    const percentText = document.createElement('span');
    percentText.textContent = `${percentage.toFixed(1)}%`;
    percentText.style.marginLeft = '10px';
    percentText.style.fontSize = '0.9em';
    
    progressBar.appendChild(progressValue);
    progressContainer.appendChild(progressBar);
    progressContainer.appendChild(percentText);
    ratioCell.appendChild(progressContainer);
    
    row.appendChild(ratioCell);
    tableBody.appendChild(row);
  });
  
  if (sortedCategories.length === 0) {
    const noDataText = window.t ? window.t('messages.noData') : '데이터가 없습니다';
    tableBody.innerHTML = `<tr><td colspan="4" style="text-align: center; color: #999;">${noDataText}</td></tr>`;
  }
}

// window 객체에 함수 등록
window.validateChartDates = validateChartDates;
window.setChartDateRange = setChartDateRange;
window.fetchDailyChartData = fetchDailyChartData;
window.initDailyChartTab = initDailyChartTab;
window.renderDailyChart = renderDailyChart;
window.showDailyChartDetail = showDailyChartDetail;
window.closeDailyChartDetail = closeDailyChartDetail;
window.handleDailyDetailSubTab = handleDailyDetailSubTab;
window.filterDailyDetailByCategory = filterDailyDetailByCategory;
window.initializeCategoryDropdown = initializeCategoryDropdown;
window.checkCategoryDataAvailability = checkCategoryDataAvailability;
window.extractPostId = extractPostId;
window.updateCategoryTable = updateCategoryTable;
window.dailyChart = dailyChart;
window.dailyChartData = dailyChartData;
window.dailyDetailData = dailyDetailData;