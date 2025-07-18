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

// 페이지 로드시 처음 데이터 가져오기
document.addEventListener('DOMContentLoaded', function() {
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
  const timeString = now.toLocaleTimeString();
  document.getElementById(elementId).textContent = `마지막 업데이트: ${timeString}`;
}

// 날짜 유효성 검사
function validateDates() {
  const startDate = document.getElementById('start-date').value;
  const endDate = document.getElementById('end-date').value;

  if (startDate && endDate && new Date(startDate) > new Date(endDate)) {
    alert('🚨 시작일이 종료일보다 늦을 수 없어요!');
    document.getElementById('end-date').value = startDate;
  }
}

// 기간 날짜 유효성 검사
function validatePeriodDates(period) {
  const startDate = document.getElementById(`period-${period}-start`).value;
  const endDate = document.getElementById(`period-${period}-end`).value;

  if (startDate && endDate && new Date(startDate) > new Date(endDate)) {
    alert(`🚨 기간 ${period.toUpperCase()}의 시작일이 종료일보다 늦을 수 없어요!`);
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
    const title = (item.pageTitle || '(제목 없음)').toLowerCase();
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

// 자동 업데이트 리스너 초기화
function initAutoUpdateListener() {
  document.getElementById('auto-update-check').addEventListener('change', function() {
    console.log('체크박스 상태 변경: ', this.checked);

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
      console.log('⭐정확한 날짜 비교:', endDate, today, endDate === today);

      if (endDate === today) {
        console.log('🔄 10초 자동 업데이트 설정 성공!!');
        // 10초마다 자동 업데이트 설정
        customDateInterval = setInterval(fetchCustomDateData, 10000);
      }
    }
  });
}