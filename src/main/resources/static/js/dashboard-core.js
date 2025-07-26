// 핵심 전역 변수 및 초기화 함수

// 전역 변수
let todayIntervalId = null;
let last30minIntervalId = null;
let wpCategoryData = null; // 워드프레스 카테고리 데이터 저장
let currentPageViewsData = {}; // 현재 페이지뷰 데이터 저장
let customDateInterval = null;

// 기간 비교 데이터 캐시
let comparisonDataCache = {
  periodA: null,
  periodB: null,
  currentFilter: 'all'
};

// 테마 관리
let currentTheme = localStorage.getItem('theme') || 'dark';

function initTheme() {
  if (currentTheme === 'light') {
    document.body.classList.add('light-theme');
    document.getElementById('theme-stylesheet').href = '/css/dashboard-light-theme.css';
    document.querySelector('.theme-icon').textContent = '☀️';
  } else {
    document.body.classList.remove('light-theme');
    document.getElementById('theme-stylesheet').href = '/css/dashboard-dark-theme.css';
    document.querySelector('.theme-icon').textContent = '🌙';
  }
}

function toggleTheme() {
  currentTheme = currentTheme === 'dark' ? 'light' : 'dark';
  localStorage.setItem('theme', currentTheme);
  
  if (currentTheme === 'light') {
    document.body.classList.add('light-theme');
    document.getElementById('theme-stylesheet').href = '/css/dashboard-light-theme.css';
    document.querySelector('.theme-icon').textContent = '☀️';
  } else {
    document.body.classList.remove('light-theme');
    document.getElementById('theme-stylesheet').href = '/css/dashboard-dark-theme.css';
    document.querySelector('.theme-icon').textContent = '🌙';
  }
  
  // 백링크 차트 테마 업데이트
  if (typeof updateBacklinkChartTheme === 'function') {
    setTimeout(updateBacklinkChartTheme, 100); // CSS 로드 후 실행
  }
}

// 페이지 로드시 처음 데이터 가져오기
document.addEventListener('DOMContentLoaded', function() {
  // 테마 초기화
  initTheme();
  
  // 언어 초기화
  if (window.updateAllTranslations) {
    window.updateAllTranslations();
  }
  
  // 날짜 입력 필드 언어 설정
  if (window.updateDateInputLanguage) {
    window.updateDateInputLanguage();
  }
  
  // 페이지 로드 시 첫 번째 탭(오늘 전체) 활성화
  fetchTodayData();
  startTodayInterval();

  // 날짜 선택기 초기화
  const now = new Date();
  const today = now.getFullYear() + '-' +
      String(now.getMonth() + 1).padStart(2, '0') + '-' +
      String(now.getDate()).padStart(2, '0');

  document.getElementById('start-date').value = today;
  document.getElementById('end-date').value = today;

  document.getElementById('auto-update-check').addEventListener('change', function() {
    if (this.checked && document.getElementById('end-date').value === today) {
      fetchCustomDateData();
    }
  });

  // 동기화 상태 확인
  checkSyncStatus();
  
  // 워드프레스 카테고리 데이터 로드
  loadWpCategoryData();
  
  // 자동 업데이트 체크박스 이벤트 리스너 추가
  initAutoUpdateListener();
});

// 시간 업데이트 함수
function updateTime(elementId) {
  const now = new Date();
  // 현재 언어에 맞는 로케일 설정
  const locale = window.getCurrentLanguage ? 
    (window.getCurrentLanguage() === 'ko' ? 'ko-KR' : 
     window.getCurrentLanguage() === 'en' ? 'en-US' : 'zh-CN') : 'ko-KR';
  const timeString = now.toLocaleTimeString(locale);
  const lastUpdateText = window.t ? window.t('labels.lastUpdate') : '마지막 업데이트';
  document.getElementById(elementId).innerHTML = `<span data-i18n="labels.lastUpdate">${lastUpdateText}</span>: ${timeString}`;
}

// 날짜 유효성 검사
function validateDates() {
  const startDate = document.getElementById('start-date').value;
  const endDate = document.getElementById('end-date').value;

  if (startDate && endDate && new Date(startDate) > new Date(endDate)) {
    alert('🚨 ' + (window.t ? window.t('errors.dateRangeInvalid') : '시작일이 종료일보다 늦을 수 없어요!'));
    document.getElementById('end-date').value = startDate;
  }
}

// 기간 날짜 유효성 검사
function validatePeriodDates(period) {
  const startDate = document.getElementById(`period-${period}-start`).value;
  const endDate = document.getElementById(`period-${period}-end`).value;

  if (startDate && endDate && new Date(startDate) > new Date(endDate)) {
    alert('🚨 ' + (window.t ? window.tTemplate('errors.periodDateInvalid', {period: period.toUpperCase()}) : `기간 ${period.toUpperCase()}의 시작일이 종료일보다 늦을 수 없어요!`));
    document.getElementById(`period-${period}-end`).value = startDate;
  }
}

// URL에서 포스트 ID 추출 함수
function extractPostId(pagePath) {
  // /?p=59696 형식
  let match = pagePath.match(/[?&]p=(\d+)/);
  if (match) return match[1];
  
  // /12345/ 형식 (WordPress Pretty Permalink)
  match = pagePath.match(/^\/(\d+)\/?$/);
  if (match) return match[1];
  
  // /posts/12345/ 또는 /p/12345/ 같은 형식도 처리
  match = pagePath.match(/\/(\d+)\/?$/);
  if (match) return match[1];
  
  return null;
}


// 페이지 제목을 접두어로 그룹화하는 함수
function groupByPrefix(data, wordCount) {
  const groups = {};

  data.forEach(item => {
    const noTitleText = window.t ? window.t('messages.noTitle') : '(제목 없음)';
    const title = (item.pageTitle || noTitleText).toLowerCase();
    const words = title.split(' ');

    let prefix;
    if (wordCount === 1) {
      prefix = words.length > 0 ? words[0] : '';
    } else if (words.length >= wordCount) {
      prefix = words.slice(0, wordCount).join(' ');
    } else {
      prefix = title; // 단어 수가 부족한 경우 전체 제목을 사용
    }

    if (prefix) {
      if (!groups[prefix]) {
        groups[prefix] = [];
      }
      groups[prefix].push(item);
    }
  });

  return groups;
}

// 현재 콘텐츠 새로고침 함수
window.refreshCurrentContent = function() {
  // 모든 업데이트 시간 요소를 다시 업데이트
  const updateTimeElements = [
    'today-update-time',
    'last30min-update-time', 
    'custom-date-update-time'
  ];
  
  // 각 요소가 존재하면 시간을 다시 업데이트 (AM/PM 변경을 위해)
  updateTimeElements.forEach(elementId => {
    const element = document.getElementById(elementId);
    if (element && element.innerHTML) {
      updateTime(elementId);
    }
  });
  
  // 백링크 업데이트 시간도 갱신
  const backlinkTimeElement = document.getElementById('backlink-update-time');
  if (backlinkTimeElement && !backlinkTimeElement.textContent.endsWith(': -')) {
    if (typeof updateBacklinkTime === 'function') {
      updateBacklinkTime();
    }
  }
};

// 자동 업데이트 리스너 초기화
function initAutoUpdateListener() {
  document.getElementById('auto-update-check').addEventListener('change', function() {
    // 이전 타이머 초기화
    if (customDateInterval) {
      clearInterval(customDateInterval);
      customDateInterval = null;
    }

    if (this.checked) {
      // 체크했을 때 일단 바로 데이터 가져오기
      fetchCustomDateData();

      // 현재 시스템 날짜 가져오기
      const now = new Date();
      const today = now.getFullYear() + '-' +
          String(now.getMonth() + 1).padStart(2, '0') + '-' +
          String(now.getDate()).padStart(2, '0');

      const endDate = document.getElementById('end-date').value;

      if (endDate === today) {
        // 10초마다 자동 업데이트 설정
        customDateInterval = setInterval(fetchCustomDateData, 10000);
      }
    }
  });
}

// 리포트 탭 초기화
function initReportTab() {
  // 날짜 필드 초기화 (3일 전 ~ 3일 전)
  const threeDaysAgo = new Date();
  threeDaysAgo.setDate(threeDaysAgo.getDate() - 3); // 3일 전
  
  const formatDate = (date) => {
    return date.getFullYear() + '-' +
        String(date.getMonth() + 1).padStart(2, '0') + '-' +
        String(date.getDate()).padStart(2, '0');
  };
  
  // 날짜 필드가 비어있을 때만 초기값 설정
  if (!document.getElementById('report-start-date').value) {
    document.getElementById('report-start-date').value = formatDate(threeDaysAgo);
    document.getElementById('report-end-date').value = formatDate(threeDaysAgo);
  }
  
  // 수신자 이메일 로드
  loadRecipientEmail();
}

// 리포트 날짜 유효성 검사
function validateReportDates() {
  const startDate = document.getElementById('report-start-date').value;
  const endDate = document.getElementById('report-end-date').value;

  if (startDate && endDate && new Date(startDate) > new Date(endDate)) {
    alert('🚨 ' + (window.t ? window.t('errors.dateRangeInvalid') : '시작일이 종료일보다 늦을 수 없어요!'));
    document.getElementById('report-end-date').value = startDate;
  }
}


// 수신자 이메일 로드
function loadRecipientEmail() {
  // 실제로는 API에서 가져와야 하지만, 여기서는 더미 데이터 표시
  const recipientEmail = document.getElementById('recipient-email');
  if (recipientEmail) {
    // TODO: API 엔드포인트에서 실제 수신자 정보 가져오기
    recipientEmail.textContent = 'admin@example.com';
  }
}

// 리포트 발송
function sendReport() {
  const startDate = document.getElementById('report-start-date').value;
  const endDate = document.getElementById('report-end-date').value;
  
  if (!startDate || !endDate) {
    alert('❓ ' + (window.t ? window.t('errors.selectAllDates') : '시작일과 종료일을 모두 선택해주세요!'));
    return;
  }
  
  // 3일 지연 validation
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const threeDaysAgo = new Date(today);
  threeDaysAgo.setDate(threeDaysAgo.getDate() - 3);
  
  const endDateObj = new Date(endDate);
  endDateObj.setHours(0, 0, 0, 0);
  
  if (endDateObj > threeDaysAgo) {
    const maxDateStr = threeDaysAgo.getFullYear() + '-' +
        String(threeDaysAgo.getMonth() + 1).padStart(2, '0') + '-' +
        String(threeDaysAgo.getDate()).padStart(2, '0');
    
    alert('⚠️ ' + (window.t ? window.tTemplate('errors.searchConsoleDelay', {maxDate: maxDateStr}) : 
      `구글 Search Console 데이터는 3일의 지연이 있습니다.\n종료일은 ${maxDateStr} 이전이어야 합니다.`));
    return;
  }
  
  // 버튼 비활성화
  const sendBtn = document.getElementById('send-report-btn');
  sendBtn.disabled = true;
  
  // 상태 영역 표시
  const statusArea = document.getElementById('report-status');
  const loadingDiv = document.getElementById('report-loading');
  const successDiv = document.getElementById('report-success');
  const errorDiv = document.getElementById('report-error');
  
  // 모든 상태 숨기기
  successDiv.style.display = 'none';
  errorDiv.style.display = 'none';
  
  // 로딩 표시
  statusArea.style.display = 'block';
  loadingDiv.style.display = 'block';
  
  // API 호출
  fetch(`/email-search-insights-report?fromDate=${startDate}&toDate=${endDate}`)
    .then(response => {
      if (!response.ok) {
        throw new Error('Network response was not ok');
      }
      return response.text();
    })
    .then(data => {
      // 로딩 숨기고 성공 표시
      loadingDiv.style.display = 'none';
      successDiv.style.display = 'block';
      
      // 성공 메시지에 날짜 정보 표시
      const successDetail = document.getElementById('report-success-detail');
      successDetail.textContent = `${startDate} ~ ${endDate} 기간의 리포트가 발송되었습니다.`;
      
      // 3초 후 자동으로 상태 숨기기
      setTimeout(() => {
        statusArea.style.display = 'none';
      }, 5000);
    })
    .catch(error => {
      console.error('리포트 발송 실패:', error);
      
      // 로딩 숨기고 에러 표시
      loadingDiv.style.display = 'none';
      errorDiv.style.display = 'block';
      
      // 에러 메시지 표시
      const errorDetail = document.getElementById('report-error-detail');
      errorDetail.textContent = '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
    })
    .finally(() => {
      // 버튼 다시 활성화
      sendBtn.disabled = false;
    });
}

// window 객체에 함수 등록
window.initReportTab = initReportTab;
window.validateReportDates = validateReportDates;
window.loadRecipientEmail = loadRecipientEmail;
window.sendReport = sendReport;