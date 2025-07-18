// 테이블 렌더링 관련 함수들

// 전체 페이지 제목별 조회수 테이블 업데이트
function updatePageViewsTable(data, tableId) {
  const tableBody = document.getElementById(tableId);
  tableBody.innerHTML = '';

  // 총 조회수 계산
  const totalViews = data.reduce((sum, item) => sum + item.pageViews, 0);

  // 데이터 정렬 (조회수 기준 내림차순)
  data.sort((a, b) => b.pageViews - a.pageViews);

  // 테이블에 데이터 추가
  data.forEach((item, index) => {
    const row = document.createElement('tr');

    // 순번 셀
    const numCell = document.createElement('td');
    numCell.textContent = index + 1;
    row.appendChild(numCell);

    // 페이지 제목 셀
    const titleCell = document.createElement('td');
    titleCell.textContent = item.pageTitle || (window.t ? window.t('messages.noTitle') : '(제목 없음)');
    row.appendChild(titleCell);

    // 조회수 셀
    const viewsCell = document.createElement('td');
    viewsCell.textContent = Math.round(item.pageViews);
    row.appendChild(viewsCell);

    // 비율 셀 (프로그레스 바)
    const ratioCell = document.createElement('td');
    const percentage = totalViews > 0 ? (item.pageViews / totalViews * 100) : 0;

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

    progressBar.appendChild(progressValue);
    progressContainer.appendChild(progressBar);
    progressContainer.appendChild(percentText);
    ratioCell.appendChild(progressContainer);

    row.appendChild(ratioCell);
    tableBody.appendChild(row);
  });
}

// 접두어별 조회수 테이블 업데이트
function updatePrefixViewsTable(data, tableId, wordCount) {
  const tableBody = document.getElementById(tableId);
  tableBody.innerHTML = '';

  // 페이지 제목을 접두어로 그룹화
  const prefixGroups = groupByPrefix(data, wordCount);

  // 총 조회수 계산
  const totalViews = data.reduce((sum, item) => sum + item.pageViews, 0);

  // 정렬된 접두어 배열 (조회수 기준 내림차순)
  const sortedPrefixes = Object.keys(prefixGroups).sort((a, b) => {
    const sumA = prefixGroups[a].reduce((sum, item) => sum + item.pageViews, 0);
    const sumB = prefixGroups[b].reduce((sum, item) => sum + item.pageViews, 0);
    return sumB - sumA;
  });

  // 테이블에 데이터 추가
  sortedPrefixes.forEach((prefix, index) => {
    const items = prefixGroups[prefix];
    const prefixTotalViews = items.reduce((sum, item) => sum + item.pageViews, 0);

    const row = document.createElement('tr');

    // 순번 셀
    const numCell = document.createElement('td');
    numCell.textContent = index + 1;
    row.appendChild(numCell);

    // 접두어 셀
    const prefixCell = document.createElement('td');
    prefixCell.textContent = prefix;
    row.appendChild(prefixCell);

    // 조회수 셀
    const viewsCell = document.createElement('td');
    viewsCell.textContent = Math.round(prefixTotalViews);
    row.appendChild(viewsCell);

    // 비율 셀 (프로그레스 바)
    const ratioCell = document.createElement('td');
    const percentage = totalViews > 0 ? (prefixTotalViews / totalViews * 100) : 0;

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

    progressBar.appendChild(progressValue);
    progressContainer.appendChild(progressBar);
    progressContainer.appendChild(percentText);
    ratioCell.appendChild(progressContainer);

    row.appendChild(ratioCell);
    tableBody.appendChild(row);
  });
}

// 카테고리별 조회수 테이블 업데이트
function updateCategoryViewsTable(categoryViews, tableId) {
  const tableBody = document.getElementById(tableId);
  tableBody.innerHTML = '';

  // 동기화 배너 표시 여부 설정
  const mainTab = tableId.split('-')[0];
  const bannerId = "sync-banner-" + mainTab;
  const syncBanner = document.getElementById(bannerId);

  if (!categoryViews || Object.keys(categoryViews).length === 0) {
    if (syncBanner) {
      syncBanner.classList.add('visible');
    }

    const row = document.createElement('tr');
    const cell = document.createElement('td');
    cell.setAttribute('colspan', '4');
    const noDataText = window.t ? window.t('messages.noData') : '데이터가 없거나 아직 카테고리가 동기화되지 않았어요!';
    cell.textContent = noDataText + ' 🥺';
    cell.style.textAlign = 'center';
    row.appendChild(cell);
    tableBody.appendChild(row);
    return;
  }

  // 데이터가 있으면 배너 숨기기
  if (syncBanner) {
    syncBanner.classList.remove('visible');
  }

  // 카테고리별 포스트 개수 계산
  const categoryPostCounts = {};
  if (wpCategoryData && wpCategoryData.posts && wpCategoryData.categories) {
    Object.entries(wpCategoryData.posts).forEach(([postId, categoryIds]) => {
      if (categoryIds && Array.isArray(categoryIds)) {
        categoryIds.forEach(catId => {
          const categoryName = wpCategoryData.categories[catId];
          if (categoryName) {
            categoryPostCounts[categoryName] = (categoryPostCounts[categoryName] || 0) + 1;
          }
        });
      }
    });
  }

  // 총 조회수 계산
  const totalViews = Object.values(categoryViews).reduce((sum, views) => sum + views, 0);

  // 배열로 변환하여 정렬 (조회수 기준 내림차순)
  const sortedCategories = Object.entries(categoryViews)
  .sort((a, b) => b[1] - a[1]);

  // 테이블에 데이터 추가
  sortedCategories.forEach(([category, views], index) => {
    const row = document.createElement('tr');

    // 순번 셀
    const numCell = document.createElement('td');
    numCell.textContent = index + 1;
    row.appendChild(numCell);

    // 카테고리 셀 (포스트 개수 포함)
    const categoryCell = document.createElement('td');
    const postCount = categoryPostCounts[category] || 0;
    categoryCell.textContent = `${category} (${postCount})`;
    row.appendChild(categoryCell);

    // 조회수 셀
    const viewsCell = document.createElement('td');
    viewsCell.textContent = Math.round(views);
    row.appendChild(viewsCell);

    // 비율 셀 (프로그레스 바)
    const ratioCell = document.createElement('td');
    const percentage = totalViews > 0 ? (views / totalViews * 100) : 0;

    const progressContainer = document.createElement('div');
    progressContainer.style.display = 'flex';
    progressContainer.style.alignItems = 'center';

    const progressBar = document.createElement('div');
    progressBar.className = 'progress-bar';
    progressBar.style.width = '100px';

    const progressValue = document.createElement('div');
    progressValue.className = 'progress-value';
    progressValue.style.width = `${percentage}%`;
    progressValue.style.background = 'linear-gradient(90deg, #b794f6 0%, #9f7aea 100%)';

    const percentText = document.createElement('span');
    percentText.textContent = `${percentage.toFixed(1)}%`;

    progressBar.appendChild(progressValue);
    progressContainer.appendChild(progressBar);
    progressContainer.appendChild(percentText);
    ratioCell.appendChild(progressContainer);

    row.appendChild(ratioCell);
    tableBody.appendChild(row);
  });
}

// 카테고리별 비교 테이블 업데이트
function updateComparisonCategoryTable(dataA, dataB, filterType = 'all') {
  if (!dataA || !dataB || !wpCategoryData) {
    return;
  }

  const categoryDataA = dataA.categoryViews || {};
  const categoryDataB = dataB.categoryViews || {};

  // 전체 조회수 계산
  const totalViewsA = Object.values(categoryDataA).reduce((sum, views) => sum + views, 0);
  const totalViewsB = Object.values(categoryDataB).reduce((sum, views) => sum + views, 0);

  // 모든 카테고리 수집
  const allCategories = new Set([
    ...Object.keys(categoryDataA),
    ...Object.keys(categoryDataB)
  ]);

  // 카테고리별 비교 데이터 생성
  let comparisonData = Array.from(allCategories).map(category => {
    const viewsA = categoryDataA[category] || 0;
    const viewsB = categoryDataB[category] || 0;
    const change = viewsB - viewsA;
    const changeRate = viewsA > 0 ? ((viewsB - viewsA) / viewsA * 100) : (viewsB > 0 ? 100 : 0);
    const shareA = totalViewsA > 0 ? (viewsA / totalViewsA * 100) : 0;
    const shareB = totalViewsB > 0 ? (viewsB / totalViewsB * 100) : 0;

    return {
      category,
      viewsA: Math.round(viewsA),
      viewsB: Math.round(viewsB),
      change,
      changeRate,
      shareA,
      shareB
    };
  });

  // 필터 적용
  comparisonData = applyFilterToData(comparisonData, filterType);

  // 테이블 업데이트
  const tableBody = document.getElementById('comparison-category-table');
  tableBody.innerHTML = '';

  if (comparisonData.length === 0) {
    tableBody.innerHTML = '<tr><td colspan="8" style="text-align: center;">😢 ' + (window.t ? window.t('errors.noMatchingData') : '조건에 맞는 데이터가 없어요!') + '</td></tr>';
    return;
  }

  comparisonData.forEach((item, index) => {
    const row = document.createElement('tr');
    row.className = 'expandable-row';
    row.onclick = () => showCategoryDetail(item.category);

    // 순번
    const numCell = document.createElement('td');
    numCell.textContent = index + 1;
    row.appendChild(numCell);

    // 카테고리명
    const categoryCell = document.createElement('td');
    categoryCell.innerHTML = `${item.category} <small style="color: #666;">${window.t ? window.t('ui.clickForDetail') : '(클릭하여 상세보기)'}</small>`;
    row.appendChild(categoryCell);

    // 기간 A
    const viewsACell = document.createElement('td');
    if (item.viewsA > item.viewsB) {
      viewsACell.innerHTML = `<strong>${item.viewsA.toLocaleString()}</strong>`;
    } else {
      viewsACell.textContent = item.viewsA.toLocaleString();
    }
    row.appendChild(viewsACell);

    // 기간 B
    const viewsBCell = document.createElement('td');
    if (item.viewsB > item.viewsA) {
      viewsBCell.innerHTML = `<strong>${item.viewsB.toLocaleString()}</strong>`;
    } else {
      viewsBCell.textContent = item.viewsB.toLocaleString();
    }
    row.appendChild(viewsBCell);

    // 변화량
    const changeCell = document.createElement('td');
    if (item.change > 0) {
      changeCell.innerHTML = `<span class="increase">+${item.change.toLocaleString()}↑</span>`;
    } else if (item.change < 0) {
      changeCell.innerHTML = `<span class="decrease">${item.change.toLocaleString()}↓</span>`;
    } else {
      changeCell.innerHTML = '<span class="unchanged">0</span>';
    }
    row.appendChild(changeCell);

    // 변화율
    const changeRateCell = document.createElement('td');
    if (item.changeRate > 0) {
      changeRateCell.innerHTML = `<span class="change-badge increase">+${item.changeRate.toFixed(1)}%</span>`;
    } else if (item.changeRate < 0) {
      changeRateCell.innerHTML = `<span class="change-badge decrease">${item.changeRate.toFixed(1)}%</span>`;
    } else {
      changeRateCell.innerHTML = '<span class="unchanged">0%</span>';
    }
    row.appendChild(changeRateCell);

    // 기간 A 비중
    const shareACell = document.createElement('td');
    if (item.shareA > item.shareB) {
      shareACell.innerHTML = `<span style="color: #2196F3;"><strong>${item.shareA.toFixed(1)}%</strong></span>`;
    } else {
      shareACell.innerHTML = `<span style="color: #2196F3;">${item.shareA.toFixed(1)}%</span>`;
    }
    row.appendChild(shareACell);

    // 기간 B 비중
    const shareBCell = document.createElement('td');
    if (item.shareB > item.shareA) {
      shareBCell.innerHTML = `<span style="color: #FF9800;"><strong>${item.shareB.toFixed(1)}%</strong></span>`;
    } else {
      shareBCell.innerHTML = `<span style="color: #FF9800;">${item.shareB.toFixed(1)}%</span>`;
    }
    row.appendChild(shareBCell);

    tableBody.appendChild(row);
  });
}

// 필터 적용 함수
function applyFilterToData(data, filterType) {
  switch (filterType) {
    case 'top-increase':
      return data
        .filter(item => item.change > 0)
        .sort((a, b) => b.change - a.change)
        .slice(0, 25);
    
    case 'top-decrease':
      return data
        .filter(item => item.change < 0)
        .sort((a, b) => a.change - b.change)
        .slice(0, 25);
    
    case 'rapid-growth':
      return data.filter(item => item.changeRate >= 100);
    
    default:
      return data.sort((a, b) => b.change - a.change);
  }
}

// 카테고리 상세 보기
function showCategoryDetail(categoryName) {
  if (!comparisonDataCache.periodA || !comparisonDataCache.periodB || !wpCategoryData) {
    return;
  }

  // 해당 카테고리의 카테고리 ID 찾기
  let categoryId = null;
  Object.entries(wpCategoryData.categories).forEach(([id, name]) => {
    if (name.replace(/&amp;/g, '&') === categoryName) {
      categoryId = id;
    }
  });

  if (!categoryId) return;

  // 해당 카테고리의 포스트들 필터링
  const postsInCategory = Object.entries(wpCategoryData.posts)
    .filter(([postId, categoryIds]) => categoryIds && categoryIds.includes(parseInt(categoryId)))
    .map(([postId]) => postId);

  // 포스트별 조회수 데이터 수집
  const postDataA = {};
  const postDataB = {};

  comparisonDataCache.periodA.pageViews.forEach(page => {
    const postId = extractPostId(page.pagePath);
    if (postId && postsInCategory.includes(postId)) {
      postDataA[postId] = {
        title: page.pageTitle,
        views: page.pageViews
      };
    }
  });

  comparisonDataCache.periodB.pageViews.forEach(page => {
    const postId = extractPostId(page.pagePath);
    if (postId && postsInCategory.includes(postId)) {
      postDataB[postId] = {
        title: page.pageTitle,
        views: page.pageViews
      };
    }
  });

  // 모든 포스트 수집
  const allPostIds = new Set([...Object.keys(postDataA), ...Object.keys(postDataB)]);
  
  // 포스트별 비교 데이터 생성
  const postComparison = Array.from(allPostIds).map(postId => {
    const noTitleText = window.t ? window.t('messages.noTitle') : '(제목 없음)';
    const dataA = postDataA[postId] || { title: noTitleText, views: 0 };
    const dataB = postDataB[postId] || { title: dataA.title, views: 0 };
    const change = dataB.views - dataA.views;
    const changeRate = dataA.views > 0 ? ((dataB.views - dataA.views) / dataA.views * 100) : (dataB.views > 0 ? 100 : 0);

    return {
      title: dataB.title || dataA.title,
      viewsA: Math.round(dataA.views),
      viewsB: Math.round(dataB.views),
      change,
      changeRate
    };
  }).sort((a, b) => b.change - a.change);

  // 상세 테이블 표시
  const detailBox = document.getElementById('comparison-post-detail');
  const detailTitle = document.getElementById('comparison-post-detail-title');
  const tableBody = document.getElementById('comparison-post-table');

  detailTitle.textContent = '📈 ' + (window.t ? window.tTemplate('ui.categoryPostDetail', {category: categoryName}) : `"${categoryName}" 카테고리 포스트별 상세 비교`);
  detailBox.style.display = 'block';
  
  tableBody.innerHTML = '';
  postComparison.forEach((post, index) => {
    const row = document.createElement('tr');

    // 순번
    const numCell = document.createElement('td');
    numCell.textContent = index + 1;
    row.appendChild(numCell);

    // 포스트 제목
    const titleCell = document.createElement('td');
    titleCell.textContent = post.title;
    row.appendChild(titleCell);

    // 기간 A
    const viewsACell = document.createElement('td');
    if (post.viewsA > post.viewsB) {
      viewsACell.innerHTML = `<strong>${post.viewsA.toLocaleString()}</strong>`;
    } else {
      viewsACell.textContent = post.viewsA.toLocaleString();
    }
    row.appendChild(viewsACell);

    // 기간 B
    const viewsBCell = document.createElement('td');
    if (post.viewsB > post.viewsA) {
      viewsBCell.innerHTML = `<strong>${post.viewsB.toLocaleString()}</strong>`;
    } else {
      viewsBCell.textContent = post.viewsB.toLocaleString();
    }
    row.appendChild(viewsBCell);

    // 변화량
    const changeCell = document.createElement('td');
    if (post.change > 0) {
      changeCell.innerHTML = `<span class="increase">+${post.change.toLocaleString()}↑</span>`;
    } else if (post.change < 0) {
      changeCell.innerHTML = `<span class="decrease">${post.change.toLocaleString()}↓</span>`;
    } else {
      changeCell.innerHTML = '<span class="unchanged">0</span>';
    }
    row.appendChild(changeCell);

    // 변화율
    const changeRateCell = document.createElement('td');
    if (post.changeRate > 0) {
      changeRateCell.innerHTML = `<span class="change-badge increase">+${post.changeRate.toFixed(1)}%</span>`;
    } else if (post.changeRate < 0) {
      changeRateCell.innerHTML = `<span class="change-badge decrease">${post.changeRate.toFixed(1)}%</span>`;
    } else {
      changeRateCell.innerHTML = '<span class="unchanged">0%</span>';
    }
    row.appendChild(changeRateCell);

    tableBody.appendChild(row);
  });

  // 스크롤
  detailBox.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}