// 데이터 가져오기 관련 함수들

// 캐시 객체 - 데이터와 타임스탬프 저장
const dataCache = {
  today: { data: null, timestamp: 0 },
  last30min: { data: null, timestamp: 0 },
  customDate: new Map() // 날짜별로 캐싱
};

// 캐시 유효 시간 (밀리초)
const CACHE_DURATION = {
  today: 10000, // 10초
  last30min: 30000, // 30초
  customDate: 60000 // 60초
};

// 캐시가 유효한지 확인
function isCacheValid(cacheEntry, duration) {
  if (!cacheEntry.data) return false;
  return (Date.now() - cacheEntry.timestamp) < duration;
}

// 오늘 데이터 가져오기 (캐싱 적용)
function fetchTodayData() {
  // 캐시가 유효하면 캐시된 데이터 사용
  if (isCacheValid(dataCache.today, CACHE_DURATION.today)) {
    console.log('📦 Using cached today data');
    processTodayData(dataCache.today.data);
    return;
  }

  fetch('/api/realtime-pageviews')
  .then(response => response.json())
  .then(data => {
    // 캐시에 저장
    dataCache.today = { data: data, timestamp: Date.now() };
    processTodayData(data);
  })
  .catch(error => console.error((window.t ? window.t('console.todayDataFetchFailed') : '오늘 데이터 가져오기 실패') + ':', error));
}

// 오늘 데이터 처리 함수 (중복 코드 제거)
function processTodayData(data) {
  // 데이터 저장
  currentPageViewsData.today = data.pageViews;
  
  // 활성 사용자 및 전체 페이지뷰 업데이트
  document.getElementById('today-active-users').textContent = data.activeUsers;

  // 전체 조회수 계산 및 표시
  const totalPageViews = data.pageViews.reduce((sum, item) => sum + item.pageViews, 0);
  document.getElementById('today-total-pageviews').textContent = Math.round(totalPageViews);

  updateTime('today-update-time');

  // 현재 활성화된 서브탭 확인
  const activeSubTab = document.querySelector('#today-content .sub-tab.active');
  const isFullTabActive = activeSubTab && activeSubTab.textContent.includes('전체 제목');
  
  // 카테고리 필터 상태 확인
  const selectedCategory = document.getElementById('today-full-category-select')?.value;
  
  // 전체 제목 탭이 활성화되어 있고 카테고리가 선택되어 있으면 필터링된 데이터 표시
  if (isFullTabActive && selectedCategory) {
    // 필터링 적용
    filterByCategoryInFullTab('today', selectedCategory);
  } else {
    // 필터링이 없으면 전체 데이터 표시
    updatePageViewsTable(data.pageViews, 'today-page-title-views');
  }

  // 접두어별 테이블 업데이트
  updatePrefixViewsTable(data.pageViews, 'today-prefix1-views', 1);
  updatePrefixViewsTable(data.pageViews, 'today-prefix2-views', 2);
  updatePrefixViewsTable(data.pageViews, 'today-prefix3-views', 3);

  // 카테고리별 테이블 업데이트
  updateCategoryViewsTable(data.categoryViews, 'today-category-views');
}

// 최근 30분 데이터 가져오기 (캐싱 적용)
function fetchLast30MinData() {
  // 캐시가 유효하면 캐시된 데이터 사용
  if (isCacheValid(dataCache.last30min, CACHE_DURATION.last30min)) {
    console.log('📦 Using cached last 30min data');
    processLast30MinData(dataCache.last30min.data);
    return;
  }

  fetch('/api/last30min-pageviews')
  .then(response => response.json())
  .then(data => {
    // 캐시에 저장
    dataCache.last30min = { data: data, timestamp: Date.now() };
    processLast30MinData(data);
  })
  .catch(error => console.error((window.t ? window.t('console.last30minDataFetchFailed') : '최근 30분 데이터 가져오기 실패') + ':', error));
}

// 최근 30분 데이터 처리 함수
function processLast30MinData(data) {
  // 활성 사용자 및 전체 페이지뷰 업데이트
  document.getElementById('last30min-active-users').textContent = data.activeUsers;

  // 전체 조회수 계산 및 표시
  const totalPageViews = data.pageViews.reduce((sum, item) => sum + item.pageViews, 0);
  document.getElementById('last30min-total-pageviews').textContent = Math.round(totalPageViews);

  updateTime('last30min-update-time');

  // 전체 테이블 업데이트
  updatePageViewsTable(data.pageViews, 'last30min-page-title-views');

  // 접두어별 테이블 업데이트
  updatePrefixViewsTable(data.pageViews, 'last30min-prefix1-views', 1);
  updatePrefixViewsTable(data.pageViews, 'last30min-prefix2-views', 2);
  updatePrefixViewsTable(data.pageViews, 'last30min-prefix3-views', 3);
}

// 날짜 지정 데이터 가져오기 (캐싱 적용)
function fetchCustomDateData() {
  const startDate = document.getElementById('start-date').value;
  const endDate = document.getElementById('end-date').value;

  if (!startDate || !endDate) {
    alert('❓ ' + (window.t ? window.t('errors.selectAllDates') : '시작일과 종료일을 모두 선택해주세요!'));
    return;
  }

  // 캐시 키 생성
  const cacheKey = `${startDate}_${endDate}`;
  const cachedEntry = dataCache.customDate.get(cacheKey);

  // 캐시가 유효하면 캐시된 데이터 사용
  if (cachedEntry && (Date.now() - cachedEntry.timestamp) < CACHE_DURATION.customDate) {
    console.log('📦 Using cached custom date data for:', cacheKey);
    processCustomDateData(cachedEntry.data);
    return;
  }

  // 초기 메시지 숨기고 로딩 표시
  const initialMessage = document.getElementById('custom-date-initial-message');
  const loadingDiv = document.getElementById('custom-date-loading');
  const dataContainer = document.getElementById('custom-date-data-container');
  
  if (initialMessage) initialMessage.style.display = 'none';
  if (loadingDiv) loadingDiv.style.display = 'block';
  if (dataContainer) dataContainer.style.display = 'none';

  // API 요청
  fetch(`/api/custom-date-pageviews?startDate=${startDate}&endDate=${endDate}`)
  .then(response => response.json())
  .then(data => {
    // 캐시에 저장
    dataCache.customDate.set(cacheKey, { data: data, timestamp: Date.now() });
    
    // 캐시 크기 제한 (최대 10개)
    if (dataCache.customDate.size > 10) {
      const firstKey = dataCache.customDate.keys().next().value;
      dataCache.customDate.delete(firstKey);
    }
    
    processCustomDateData(data);
  })
  .catch(error => {
    console.error((window.t ? window.t('console.customDateDataFetchFailed') : '날짜 지정 데이터 가져오기 실패') + ':', error);
    // 로딩 숨기고 초기 메시지 다시 표시
    const loadingDiv = document.getElementById('custom-date-loading');
    const initialMessage = document.getElementById('custom-date-initial-message');
    if (loadingDiv) loadingDiv.style.display = 'none';
    if (initialMessage) initialMessage.style.display = 'block';
  });
}

// 날짜 지정 데이터 처리 함수
function processCustomDateData(data) {
  // 로딩 숨기고 데이터 컨테이너 표시
  const loadingDiv = document.getElementById('custom-date-loading');
  const dataContainer = document.getElementById('custom-date-data-container');
  
  if (loadingDiv) loadingDiv.style.display = 'none';
  if (dataContainer) dataContainer.style.display = 'block';
  
  // 데이터 저장
  currentPageViewsData.customDate = data.pageViews;
  
  // 활성 사용자 업데이트
  document.getElementById('custom-date-active-users').textContent = data.activeUsers;

  // 전체 조회수 계산 및 표시
  const totalPageViews = data.pageViews.reduce((sum, item) => sum + item.pageViews, 0);
  document.getElementById('custom-date-total-pageviews').textContent = Math.round(totalPageViews);

  updateTime('custom-date-update-time');

  // 현재 활성화된 서브탭 확인
  const activeSubTab = document.querySelector('#custom-date-content .sub-tab.active');
  const isFullTabActive = activeSubTab && activeSubTab.textContent.includes('전체 제목');
  
  // 카테고리 필터 상태 확인
  const selectedCategory = document.getElementById('custom-date-full-category-select')?.value;
  
  // 전체 제목 탭이 활성화되어 있고 카테고리가 선택되어 있으면 필터링된 데이터 표시
  if (isFullTabActive && selectedCategory) {
    // 필터링 적용
    filterByCategoryInFullTab('custom-date', selectedCategory);
  } else {
    // 필터링이 없으면 전체 데이터 표시
    updatePageViewsTable(data.pageViews, 'custom-date-page-title-views');
  }
  
  // 접두어별 테이블 업데이트
  updatePrefixViewsTable(data.pageViews, 'custom-date-prefix1-views', 1);
  updatePrefixViewsTable(data.pageViews, 'custom-date-prefix2-views', 2);
  updatePrefixViewsTable(data.pageViews, 'custom-date-prefix3-views', 3);

  // 카테고리별 테이블 업데이트
  updateCategoryViewsTable(data.categoryViews, 'custom-date-category-views');
}

// 기간 비교 데이터 가져오기
function fetchComparisonData() {
  // 기간 A 날짜
  const periodAStart = document.getElementById('period-a-start').value;
  const periodAEnd = document.getElementById('period-a-end').value;
  
  // 기간 B 날짜
  const periodBStart = document.getElementById('period-b-start').value;
  const periodBEnd = document.getElementById('period-b-end').value;
  
  if (!periodAStart || !periodAEnd || !periodBStart || !periodBEnd) {
    alert('❓ ' + (window.t ? window.t('errors.selectBothPeriods') : '두 기간의 시작일과 종료일을 모두 선택해주세요!'));
    return;
  }
  
  // 초기 메시지 숨기고 로딩 표시
  const initialMessage = document.getElementById('comparison-initial-message');
  const loadingDiv = document.getElementById('comparison-loading');
  const dataContainer = document.getElementById('comparison-data-container');
  
  if (initialMessage) initialMessage.style.display = 'none';
  if (loadingDiv) loadingDiv.style.display = 'block';
  if (dataContainer) dataContainer.style.display = 'none';
  
  // 두 기간의 데이터를 병렬로 가져오기
  Promise.all([
    fetch(`/api/custom-date-pageviews?startDate=${periodAStart}&endDate=${periodAEnd}`).then(r => r.json()),
    fetch(`/api/custom-date-pageviews?startDate=${periodBStart}&endDate=${periodBEnd}`).then(r => r.json())
  ])
  .then(([dataA, dataB]) => {
    // 로딩 숨기고 데이터 컨테이너 표시
    if (loadingDiv) loadingDiv.style.display = 'none';
    if (dataContainer) dataContainer.style.display = 'block';
    
    // 데이터 캐싱
    comparisonDataCache.periodA = dataA;
    comparisonDataCache.periodB = dataB;
    
    // 카테고리별 비교 테이블 업데이트
    updateComparisonCategoryTable(dataA, dataB);
    
    // 필터 초기화
    document.querySelectorAll('#comparison-filters .filter-btn').forEach(btn => {
      btn.classList.remove('active');
    });
    document.querySelector('#comparison-filters .filter-btn').classList.add('active');
    comparisonDataCache.currentFilter = 'all';
  })
  .catch(error => {
    console.error((window.t ? window.t('console.comparisonDataFetchFailed') : '비교 데이터 가져오기 실패') + ':', error);
    // 로딩 숨기고 초기 메시지 다시 표시
    if (loadingDiv) loadingDiv.style.display = 'none';
    if (initialMessage) initialMessage.style.display = 'block';
  });
}

// 비교 필터 적용
function applyComparisonFilter(filterType) {
  // 필터 버튼 활성화 상태 변경
  document.querySelectorAll('#comparison-filters .filter-btn').forEach(btn => {
    btn.classList.remove('active');
  });
  event.target.classList.add('active');
  
  // 상세 비교 창 닫기
  closeComparisonDetail();
  
  comparisonDataCache.currentFilter = filterType;
  
  if (comparisonDataCache.periodA && comparisonDataCache.periodB) {
    updateComparisonCategoryTable(
      comparisonDataCache.periodA, 
      comparisonDataCache.periodB, 
      filterType
    );
  }
}

// 카테고리별 필터링 함수
function filterByCategory(mainTab, categoryId) {
  // 드롭다운 표시 여부 결정
  const filterContainer = document.getElementById(`${mainTab}-category-filter`);
  
  if (!categoryId) {
    // 전체 카테고리 선택시 원본 데이터로 복원
    if (mainTab === 'today' && currentPageViewsData.today) {
      updatePageViewsTable(currentPageViewsData.today, 'today-category-views');
    } else if (mainTab === 'custom-date' && currentPageViewsData.customDate) {
      updatePageViewsTable(currentPageViewsData.customDate, 'custom-date-category-views');
    }
    return;
  }
  
  // 필터링된 데이터 생성
  let filteredData = [];
  let sourceData = null;
  
  if (mainTab === 'today' && currentPageViewsData.today) {
    sourceData = currentPageViewsData.today;
  } else if (mainTab === 'custom-date' && currentPageViewsData.customDate) {
    sourceData = currentPageViewsData.customDate;
  }
  
  if (sourceData && wpCategoryData) {
    filteredData = sourceData.filter(item => {
      const postId = window.extractPostId ? window.extractPostId(item.pagePath) : null;
      if (!postId) return false;
      
      const postCategories = wpCategoryData.posts[postId];
      return postCategories && postCategories.includes(parseInt(categoryId));
    });
  }
  
  // 필터링된 데이터로 테이블 업데이트
  updatePageViewsTable(filteredData, `${mainTab}-category-views`);
}

// 전체 제목 탭에서 카테고리별 필터링 함수
function filterByCategoryInFullTab(mainTab, categoryId) {
  // daily-detail의 경우 별도 처리
  if (mainTab === 'daily-detail') {
    window.filterDailyDetailByCategory(categoryId);
    return;
  }
  
  // 필터링 안 함 (전체 카테고리 선택)
  if (!categoryId) {
    // 원본 데이터로 복원
    if (mainTab === 'today' && currentPageViewsData.today) {
      updatePageViewsTable(currentPageViewsData.today, 'today-page-title-views');
    } else if (mainTab === 'custom-date' && currentPageViewsData.customDate) {
      updatePageViewsTable(currentPageViewsData.customDate, 'custom-date-page-title-views');
    }
    return;
  }
  
  // 필터링된 데이터 생성
  let filteredData = [];
  let sourceData = null;
  
  if (mainTab === 'today' && currentPageViewsData.today) {
    sourceData = currentPageViewsData.today;
  } else if (mainTab === 'custom-date' && currentPageViewsData.customDate) {
    sourceData = currentPageViewsData.customDate;
  }
  
  if (sourceData && wpCategoryData) {
    filteredData = sourceData.filter(item => {
      const postId = window.extractPostId ? window.extractPostId(item.pagePath) : null;
      if (!postId) return false;
      
      const postCategories = wpCategoryData.posts[postId];
      return postCategories && postCategories.includes(parseInt(categoryId));
    });
  }
  
  // 필터링된 데이터로 테이블 업데이트
  updatePageViewsTable(filteredData, `${mainTab}-page-title-views`);
}