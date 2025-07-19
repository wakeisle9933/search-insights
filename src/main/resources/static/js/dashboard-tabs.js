// 탭 관리 관련 함수들

// 메인 탭 전환 함수
function showMainTab(tabName) {
  // 모든 메인 탭과 탭 컨텐츠를 비활성화
  document.querySelectorAll('.main-tab').forEach(tab => {
    tab.classList.remove('active');
  });
  document.querySelectorAll('.tab-content').forEach(content => {
    content.classList.remove('active');
  });

  // 일단 모든 인터벌 중지
  stopTodayInterval();
  stopLast30minInterval();
  
  // 일간 차트 인터벌도 중지
  if (typeof stopDailyChartInterval === 'function') {
    stopDailyChartInterval();
  }

  // 선택한 메인 탭과 해당 컨텐츠를 활성화
  if (tabName === 'today') {
    document.querySelector('.main-tab:nth-child(1)').classList.add('active');
    document.getElementById('today-content').classList.add('active');
    fetchTodayData(); // 탭 전환 시 즉시 데이터 가져오기
    startTodayInterval(); // 오늘 데이터만 주기적으로 가져오기
  } else if (tabName === 'last30min') {
    document.querySelector('.main-tab:nth-child(2)').classList.add('active');
    document.getElementById('last30min-content').classList.add('active');
    startLast30minInterval(); // 최근 30분 데이터만 주기적으로 가져오기
  } else if (tabName === 'custom-date') {
    document.querySelector('.main-tab:nth-child(3)').classList.add('active');
    document.getElementById('custom-date-content').classList.add('active');

    // 오늘 포함시 자동 업데이트
    const autoUpdateCheck = document.getElementById('auto-update-check');
    if (autoUpdateCheck && autoUpdateCheck.checked) {
      const endDate = document.getElementById('end-date').value;
      const now = new Date();
      const today = now.getFullYear() + '-' +
          String(now.getMonth() + 1).padStart(2, '0') + '-' +
          String(now.getDate()).padStart(2, '0');

      if (endDate === today) {
        // 데이터 가져오기
        fetchCustomDateData();
        // 30초마다 자동 업데이트
        customDateIntervalId = setInterval(fetchCustomDateData, 60000);
      }
    }
  } else if (tabName === 'comparison') {
    document.querySelector('.main-tab:nth-child(4)').classList.add('active');
    document.getElementById('comparison-content').classList.add('active');
  } else if (tabName === 'daily-chart') {
    document.querySelector('.main-tab:nth-child(5)').classList.add('active');
    document.getElementById('daily-chart-content').classList.add('active');
    // 일간 차트는 수동 조회로 변경 - 자동 조회하지 않음
    if (typeof initDailyChartTab === 'function') {
      initDailyChartTab(); // 날짜 필드 초기화만
    }
  }
}

// 서브 탭 전환 함수
function showSubTab(mainTab, subTab) {
  // daily-detail의 경우 특별 처리
  if (mainTab === 'daily-detail') {
    const detailBox = document.getElementById('daily-chart-detail');
    const subTabs = detailBox.querySelectorAll('.sub-tab');
    const subContents = detailBox.querySelectorAll('.sub-tab-content');
    
    subTabs.forEach(tab => {
      tab.classList.remove('active');
    });
    subContents.forEach(content => {
      content.classList.remove('active');
    });
    
    // 선택한 서브 탭 활성화
    const tabTypes = ['full', 'prefix1', 'prefix2', 'prefix3', 'category'];
    const tabIndex = tabTypes.indexOf(subTab);
    if (tabIndex >= 0 && tabIndex < subTabs.length) {
      subTabs[tabIndex].classList.add('active');
      const content = document.getElementById(`${mainTab}-${subTab}-content`);
      if (content) {
        content.classList.add('active');
      }
    }
    
    // 전체 제목 탭의 카테고리 필터 드롭다운 표시/숨김
    const fullCategoryFilter = document.getElementById(`${mainTab}-full-category-filter`);
    if (fullCategoryFilter) {
      if (subTab === 'full') {
        fullCategoryFilter.classList.add('visible');
      } else {
        fullCategoryFilter.classList.remove('visible');
      }
    }
    return;
  }
  
  // 기존 메인 탭들의 처리
  const subTabs = document.querySelectorAll(`#${mainTab}-content .sub-tab`);
  const subContents = document.querySelectorAll(`#${mainTab}-content .sub-tab-content`);

  subTabs.forEach(tab => {
    tab.classList.remove('active');
  });
  subContents.forEach(content => {
    content.classList.remove('active');
  });

  // 선택한 서브 탭 활성화 (indexOf로 찾기)
  const tabTypes = ['full', 'prefix1', 'prefix2', 'prefix3', 'category'];
  const tabIndex = tabTypes.indexOf(subTab);
  if (tabIndex >= 0) {
    subTabs[tabIndex].classList.add('active');
    document.getElementById(`${mainTab}-${subTab}-content`).classList.add('active');
  }
  
  // 전체 제목 탭의 카테고리 필터 드롭다운 표시/숨김
  const fullCategoryFilter = document.getElementById(`${mainTab}-full-category-filter`);
  if (fullCategoryFilter) {
    if (subTab === 'full') {
      fullCategoryFilter.classList.add('visible');
    } else {
      fullCategoryFilter.classList.remove('visible');
    }
  }
}

// 오늘 데이터 인터벌 시작 함수
function startTodayInterval() {
  if (todayIntervalId === null) {
    todayIntervalId = setInterval(fetchTodayData, 60000); // 60초 (1분)
  }
}

// 오늘 데이터 인터벌 중지 함수
function stopTodayInterval() {
  if (todayIntervalId !== null) {
    clearInterval(todayIntervalId);
    todayIntervalId = null;
  }
}

// 최근 30분 데이터 인터벌 시작 함수
function startLast30minInterval() {
  if (last30minIntervalId === null) {
    fetchLast30MinData(); // 먼저 한 번 호출
    last30minIntervalId = setInterval(fetchLast30MinData, 60000); // 60초 (1분)
  }
}

// 최근 30분 데이터 인터벌 중지 함수
function stopLast30minInterval() {
  if (last30minIntervalId !== null) {
    clearInterval(last30minIntervalId);
    last30minIntervalId = null;
  }
}

// 빠른 날짜 선택 함수 (날짜 지정 탭용)
function setCustomDateRange(days) {
  const now = new Date();
  const excludeToday = document.getElementById('custom-date-exclude-today').checked;
  
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
  
  document.getElementById('start-date').value = formatDate(startDate);
  document.getElementById('end-date').value = formatDate(endDate);
}

// 빠른 날짜 선택 함수 (기간 비교 탭용)
function setQuickDateRange(days) {
  const today = new Date();
  const excludeToday = document.getElementById('exclude-today-check').checked;
  
  // 전일 기준 체크시 하루 전부터 시작
  if (excludeToday) {
    today.setDate(today.getDate() - 1);
  }
  
  // 기간 B 설정 (최근 기간)
  const periodBEnd = new Date(today);
  const periodBStart = new Date(today);
  periodBStart.setDate(periodBStart.getDate() - days + 1);
  
  // 기간 A 설정 (이전 기간)
  const periodAEnd = new Date(periodBStart);
  periodAEnd.setDate(periodAEnd.getDate() - 1);
  const periodAStart = new Date(periodAEnd);
  periodAStart.setDate(periodAStart.getDate() - days + 1);
  
  // 날짜 형식 변환 (YYYY-MM-DD)
  const formatDate = (date) => {
    return date.getFullYear() + '-' + 
           String(date.getMonth() + 1).padStart(2, '0') + '-' + 
           String(date.getDate()).padStart(2, '0');
  };
  
  // 날짜 입력
  document.getElementById('period-a-start').value = formatDate(periodAStart);
  document.getElementById('period-a-end').value = formatDate(periodAEnd);
  document.getElementById('period-b-start').value = formatDate(periodBStart);
  document.getElementById('period-b-end').value = formatDate(periodBEnd);
}

// 비교 상세 닫기 함수
function closeComparisonDetail() {
  document.getElementById('comparison-post-detail').style.display = 'none';
}