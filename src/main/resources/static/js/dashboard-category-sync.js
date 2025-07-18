// 카테고리 동기화 관련 함수들

// 동기화 상태 확인 함수
function checkSyncStatus() {
  fetch('/api/realtime-pageviews')
  .then(response => response.json())
  .then(data => {
    const hasCategories = data.categoryViews && Object.keys(data.categoryViews).length > 0;

    // 각 탭의 배너 표시/숨김 설정
    const todayBanner = document.getElementById('sync-banner-today');
    const customDateBanner = document.getElementById('sync-banner-custom-date');
    
    if (todayBanner) {
      todayBanner.classList.toggle('visible', !hasCategories);
    }
    if (customDateBanner) {
      customDateBanner.classList.toggle('visible', !hasCategories);
    }
  })
  .catch(error => console.error('동기화 상태 확인 실패:', error));
}

// 워드프레스 카테고리 데이터 로드 함수
function loadWpCategoryData() {
  fetch('/api/wp-categories-data')
  .then(response => {
    if (!response.ok) {
      throw new Error('카테고리 데이터를 찾을 수 없어요!');
    }
    return response.json();
  })
  .then(data => {
    wpCategoryData = data;

    // 드롭다운에 카테고리 옵션 추가
    const todayFullSelect = document.getElementById('today-full-category-select');
    const customDateFullSelect = document.getElementById('custom-date-full-category-select');
    
    // 기존 옵션 초기화 (전체 카테고리 옵션은 유지)
    if (todayFullSelect) {
      todayFullSelect.innerHTML = '<option value="">전체 카테고리</option>';
    }
    if (customDateFullSelect) {
      customDateFullSelect.innerHTML = '<option value="">전체 카테고리</option>';
    }
    
    if (!data.categories) {
      console.error('카테고리 데이터가 비어있어요!');
      return;
    }
    
    // 포스트가 있는 카테고리만 필터링
    const categoriesWithPosts = new Set();
    Object.values(data.posts).forEach(categoryIds => {
      if (categoryIds && Array.isArray(categoryIds)) {
        categoryIds.forEach(catId => categoriesWithPosts.add(catId.toString()));
      }
    });
    
    // 포스트가 있는 카테고리만 정렬 (이름 기준)
    const sortedCategories = Object.entries(data.categories)
      .filter(([id, name]) => categoriesWithPosts.has(id))
      .sort((a, b) => a[1].localeCompare(b[1]));
    
    // 드롭다운에 옵션 추가
    sortedCategories.forEach(([id, name]) => {
      // HTML 엔티티 디코딩
      const decodedName = name.replace(/&amp;/g, '&');
      
      // 오늘 전체 탭 드롭다운
      const option1 = document.createElement('option');
      option1.value = id;
      option1.textContent = decodedName;
      if (todayFullSelect) todayFullSelect.appendChild(option1);
      
      // 날짜 지정 탭 드롭다운
      const option2 = document.createElement('option');
      option2.value = id;
      option2.textContent = decodedName;
      if (customDateFullSelect) customDateFullSelect.appendChild(option2);
    });
  })
  .catch(error => {
    console.error('카테고리 데이터 로드 실패:', error);
  });
}

// 워드프레스 카테고리 동기화 함수
function syncWordPressCategories(forceFullSync = false) {
  const syncButton = document.querySelector('.sync-button');
  const fullSyncButton = document.querySelector('.full-sync-button');
  const originalText = syncButton.innerHTML;
  const originalFullSyncText = fullSyncButton.innerHTML;

  // 모든 동기화 배너를 표시
  document.querySelectorAll('.sync-banner').forEach(banner => {
    banner.textContent = forceFullSync ?
        '⏳ 전체 카테고리 동기화 중... 시간이 조금 걸릴 수 있어요!' :
        '⏳ 카테고리 동기화 중... 조금만 기다려주세요!';
    banner.classList.add('visible');
  });

  // 버튼 텍스트 변경 및 비활성화
  syncButton.innerHTML = '<span class="sync-button-icon">⏳</span> 동기화 중...';
  syncButton.disabled = true;
  fullSyncButton.innerHTML = '<span class="sync-button-icon">⏳</span> 동기화 중...';
  fullSyncButton.disabled = true;

  // API 호출 URL (forceFullSync 파라미터 추가)
  fetch(`/api/sync-wordpress-categories${forceFullSync ? '?forceFullSync=true' : ''}`)
  .then(response => response.json())
  .then(data => {
    // 모든 동기화 배너 업데이트
    document.querySelectorAll('.sync-banner').forEach(banner => {
      banner.textContent = '✅ 동기화 완료! 카테고리 데이터가 최신 상태예요! 💝';

      // 3초 후에 배너 숨기기
      setTimeout(() => {
        banner.classList.remove('visible');
      }, 3000);
    });

    // 동기화 후 데이터 다시 가져오기
    fetchTodayData();
    if (document.querySelector('#last30min-content.active')) {
      fetchLast30MinData();
    }
    
    // 카테고리 데이터 다시 로드
    loadWpCategoryData();

    // 버튼 복원
    syncButton.innerHTML = originalText;
    syncButton.disabled = false;
    fullSyncButton.innerHTML = originalFullSyncText;
    fullSyncButton.disabled = false;
  })
  .catch(error => {
    console.error('카테고리 동기화 실패:', error);

    // 모든 동기화 배너 업데이트
    document.querySelectorAll('.sync-banner').forEach(banner => {
      banner.textContent = '❌ 동기화 실패! 다시 시도해주세요! 😢';
    });

    // 버튼 복원
    syncButton.innerHTML = originalText;
    syncButton.disabled = false;
    fullSyncButton.innerHTML = originalFullSyncText;
    fullSyncButton.disabled = false;
  });
}