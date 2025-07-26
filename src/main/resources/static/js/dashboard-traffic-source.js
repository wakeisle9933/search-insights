// 트래픽 소스 관련 전역 변수
let trafficSourceData = null;

// 트래픽 소스 데이터 가져오기
async function fetchTrafficSourceData(startDate, endDate) {
    try {
        // 로딩 상태 표시
        document.getElementById('waffle-loading').style.display = 'block';
        document.getElementById('traffic-table-loading').style.display = 'block';
        document.getElementById('waffle-initial-message').style.display = 'none';
        document.getElementById('waffle-chart').style.display = 'none';
        document.getElementById('traffic-source-initial-message').style.display = 'none';
        document.getElementById('traffic-source-table').style.display = 'none';
        
        // 브라우저가 화면을 다시 그릴 시간 주기
        await new Promise(resolve => setTimeout(resolve, 10));
        
        const response = await fetch(`/api/traffic-by-source?startDate=${startDate}&endDate=${endDate}`);
        const data = await response.json();
        
        if (!response.ok) {
            throw new Error(data.message || 'Failed to fetch traffic source data');
        }
        
        trafficSourceData = data;
        
        // 와플차트 렌더링
        renderWaffleChart(data.trafficSources, data.totalSessions);
        
        // 테이블 렌더링
        renderTrafficSourceTable(data.trafficSources, data.totalSessions);
        
        // 로딩 상태 숨기기
        document.getElementById('waffle-loading').style.display = 'none';
        document.getElementById('traffic-table-loading').style.display = 'none';
        document.getElementById('waffle-chart').style.display = 'block';
        document.getElementById('traffic-source-table').style.display = 'table';
        
    } catch (error) {
        console.error('Error fetching traffic source data:', error);
        document.getElementById('waffle-loading').style.display = 'none';
        document.getElementById('traffic-table-loading').style.display = 'none';
        document.getElementById('waffle-initial-message').style.display = 'flex';
        document.getElementById('traffic-source-initial-message').style.display = 'flex';
        alert('트래픽 소스 데이터를 불러오는 중 오류가 발생했습니다: ' + error.message);
    }
}

// 도메인 이름 매핑
const domainNameMap = {
    'google': 'Google',
    'naver.com': 'Naver',
    'daum.net': 'Daum',
    'yahoo': 'Yahoo',
    'bing': 'Bing',
    '(direct)': 'Direct',
    'facebook': 'Facebook',
    'instagram': 'Instagram',
    'twitter': 'Twitter',
    'youtube': 'YouTube',
    'linkedin': 'LinkedIn',
    'pinterest': 'Pinterest',
    'reddit': 'Reddit',
    'tiktok': 'TikTok'
};

// 도메인 색상 클래스 가져오기
function getDomainColorClass(source) {
    const sourceLower = source.toLowerCase();
    
    if (sourceLower.includes('google')) return 'waffle-google';
    if (sourceLower.includes('naver')) return 'waffle-naver';
    if (sourceLower.includes('daum')) return 'waffle-daum';
    if (sourceLower.includes('yahoo')) return 'waffle-yahoo';
    if (sourceLower.includes('bing')) return 'waffle-bing';
    if (sourceLower === '(direct)') return 'waffle-direct';
    if (sourceLower.includes('facebook')) return 'waffle-facebook';
    if (sourceLower.includes('instagram')) return 'waffle-instagram';
    if (sourceLower.includes('twitter')) return 'waffle-twitter';
    if (sourceLower.includes('youtube')) return 'waffle-youtube';
    if (sourceLower.includes('linkedin')) return 'waffle-linkedin';
    if (sourceLower.includes('pinterest')) return 'waffle-pinterest';
    if (sourceLower.includes('reddit')) return 'waffle-reddit';
    if (sourceLower.includes('tiktok')) return 'waffle-tiktok';
    
    return 'waffle-other';
}

// 도메인 이름 가져오기
function getDomainDisplayName(source) {
    const sourceLower = source.toLowerCase();
    
    for (const [key, value] of Object.entries(domainNameMap)) {
        if (sourceLower.includes(key) || sourceLower === key) {
            return value;
        }
    }
    
    // 알려지지 않은 도메인은 원본 이름 표시 (첫글자만 대문자로)
    if (source && source !== '(direct)') {
        return source.charAt(0).toUpperCase() + source.slice(1).toLowerCase();
    }
    return source;
}

// 와플차트 렌더링
function renderWaffleChart(trafficSources, totalSessions) {
    const container = document.getElementById('waffle-chart');
    container.innerHTML = '';
    
    // 상위 도메인 그룹화 및 정렬
    const domainGroups = {};
    
    // 대소문자 구분 없이 그룹화
    trafficSources.forEach(item => {
        // 대소문자 통일해서 그룹 키 생성
        const displayName = getDomainDisplayName(item.source);
        const groupKey = displayName.toLowerCase(); // 그룹화용 키
        
        if (domainGroups[groupKey]) {
            domainGroups[groupKey].sessions += item.sessions;
            domainGroups[groupKey].sources.push(item);
        } else {
            domainGroups[groupKey] = {
                displayName: displayName, // 표시용 이름
                originalSource: item.source.toLowerCase(), // 색상 매칭용
                sessions: item.sessions,
                sources: [item]
            };
        }
    });
    
    // 세션 수로 정렬하고 상위 7개 선택 (Other 없이!)
    const sortedGroups = Object.values(domainGroups)
        .sort((a, b) => b.sessions - a.sessions)
        .slice(0, 7);
    
    // 100개 셀 생성
    const grid = document.createElement('div');
    grid.className = 'waffle-grid';
    
    let cellIndex = 0;
    sortedGroups.forEach(group => {
        const percentage = Math.round((group.sessions / totalSessions) * 100);
        const cellCount = percentage; // 1셀 = 1%
        
        for (let i = 0; i < cellCount && cellIndex < 100; i++) {
            const cell = document.createElement('div');
            cell.className = `waffle-cell ${getDomainColorClass(group.originalSource)}`;
            cell.dataset.source = group.displayName;
            cell.dataset.sessions = group.sessions;
            cell.dataset.percentage = percentage;
            
            // 툴팁 이벤트
            cell.addEventListener('mouseenter', showWaffleTooltip);
            cell.addEventListener('mouseleave', hideWaffleTooltip);
            
            grid.appendChild(cell);
            cellIndex++;
        }
    });
    
    // 남은 셀은 빈 셀로 채우기
    while (cellIndex < 100) {
        const cell = document.createElement('div');
        cell.className = 'waffle-cell';
        cell.style.backgroundColor = 'rgba(255, 255, 255, 0.05)';
        grid.appendChild(cell);
        cellIndex++;
    }
    
    container.appendChild(grid);
    
    // 범례 생성
    const legend = document.createElement('div');
    legend.className = 'waffle-legend';
    
    sortedGroups.forEach(group => {
        const percentage = ((group.sessions / totalSessions) * 100).toFixed(1);
        
        const legendItem = document.createElement('div');
        legendItem.className = 'waffle-legend-item';
        
        const colorBox = document.createElement('div');
        colorBox.className = `waffle-legend-color ${getDomainColorClass(group.originalSource)}`;
        
        const label = document.createElement('span');
        label.className = 'waffle-legend-label';
        label.textContent = group.displayName;
        
        const value = document.createElement('span');
        value.className = 'waffle-legend-value';
        value.textContent = `(${percentage}%)`;
        
        legendItem.appendChild(colorBox);
        legendItem.appendChild(label);
        legendItem.appendChild(value);
        legend.appendChild(legendItem);
    });
    
    container.appendChild(legend);
}

// 툴팁 표시
function showWaffleTooltip(event) {
    const cell = event.target;
    const source = cell.dataset.source;
    
    if (!source) return;
    
    const tooltip = document.createElement('div');
    tooltip.className = 'waffle-tooltip';
    tooltip.innerHTML = `
        <strong>${source}</strong><br>
        세션: ${parseInt(cell.dataset.sessions).toLocaleString()}<br>
        비율: ${cell.dataset.percentage}%
    `;
    
    document.body.appendChild(tooltip);
    
    const rect = cell.getBoundingClientRect();
    tooltip.style.left = rect.left + rect.width / 2 - tooltip.offsetWidth / 2 + 'px';
    tooltip.style.top = rect.top - tooltip.offsetHeight - 10 + 'px';
}

// 툴팁 숨기기
function hideWaffleTooltip() {
    const tooltip = document.querySelector('.waffle-tooltip');
    if (tooltip) {
        tooltip.remove();
    }
}

// 트래픽 소스 테이블 렌더링
function renderTrafficSourceTable(trafficSources, totalSessions) {
    const tbody = document.getElementById('traffic-source-tbody');
    tbody.innerHTML = '';
    
    // 대소문자 구분 없이 그룹화
    const groupedSources = {};
    
    trafficSources.forEach(source => {
        const displayName = getDomainDisplayName(source.source);
        const groupKey = displayName.toLowerCase();
        
        if (groupedSources[groupKey]) {
            groupedSources[groupKey].sessions += source.sessions;
            groupedSources[groupKey].users += source.users;
            groupedSources[groupKey].pageviews += source.pageviews;
        } else {
            groupedSources[groupKey] = {
                displayName: displayName,
                source: source.source,
                channelGroup: source.channelGroup,
                sessions: source.sessions,
                users: source.users,
                pageviews: source.pageviews
            };
        }
    });
    
    // 세션 수로 정렬하고 상위 7개만 표시
    const topSources = Object.values(groupedSources)
        .sort((a, b) => b.sessions - a.sessions)
        .slice(0, 7);
    
    topSources.forEach((source, index) => {
        const row = document.createElement('tr');
        const percentage = ((source.sessions / totalSessions) * 100).toFixed(1);
        const displayName = getDomainDisplayName(source.source);
        
        // 채널 그룹은 영어 원본 그대로 표시 (Organic Search는 Organic으로 줄임)
        const channelDisplay = source.channelGroup === 'Organic Search' ? 'Organic' : source.channelGroup;
        
        row.innerHTML = `
            <td>${index + 1}</td>
            <td>
                <span class="waffle-legend-color ${getDomainColorClass(source.source)}" 
                      style="display: inline-block; margin-right: 8px; vertical-align: middle;"></span>
                ${source.displayName}
            </td>
            <td>${channelDisplay}</td>
            <td>${source.sessions.toLocaleString()}</td>
            <td>${percentage}%</td>
        `;
        
        // 행 클릭 이벤트 추가
        row.style.cursor = 'pointer';
        row.addEventListener('click', () => {
            showSourceDetail(source.displayName, source.source);
        });
        
        // hover 효과 추가
        row.addEventListener('mouseenter', () => {
            row.style.backgroundColor = 'rgba(255, 255, 255, 0.05)';
        });
        row.addEventListener('mouseleave', () => {
            row.style.backgroundColor = '';
        });
        
        tbody.appendChild(row);
    });
}

// 도메인별 상세 데이터 표시
async function showSourceDetail(displayName, sourceDomain) {
    // 다른 상세 페이지 닫기
    const hourlyDetail = document.getElementById('hourly-detail');
    const dailyDetail = document.getElementById('daily-chart-detail');
    if (hourlyDetail && hourlyDetail.style.display !== 'none') {
        hourlyDetail.style.display = 'none';
    }
    if (dailyDetail && dailyDetail.style.display !== 'none') {
        dailyDetail.style.display = 'none';
    }
    
    // 날짜 범위 가져오기
    const startDate = document.getElementById('chart-start-date').value;
    const endDate = document.getElementById('chart-end-date').value;
    
    if (!startDate || !endDate) {
        alert('날짜 범위를 먼저 선택해주세요!');
        return;
    }
    
    // 로딩 표시
    const loadingDiv = document.getElementById('source-detail-loading');
    if (loadingDiv) loadingDiv.style.display = 'block';
    
    // 브라우저가 화면을 다시 그릴 시간 주기
    await new Promise(resolve => setTimeout(resolve, 10));
    
    try {
        // API 호출
        const response = await fetch(
            `/api/pageviews-by-source?startDate=${startDate}&endDate=${endDate}&sourceDomain=${sourceDomain}`
        );
        
        if (!response.ok) {
            throw new Error('Failed to fetch source detail data');
        }
        
        const data = await response.json();
        
        // 상세 페이지 표시
        document.getElementById('source-detail').style.display = 'block';
        
        // 제목 업데이트
        const titleElement = document.getElementById('source-detail-title');
        const lang = getCurrentLanguage ? getCurrentLanguage() : 'ko';
        
        if (lang === 'ko') {
            titleElement.textContent = `📊 ${displayName} 유입 상세 분석`;
        } else if (lang === 'en') {
            titleElement.textContent = `📊 ${displayName} Traffic Analysis`;
        } else {
            titleElement.textContent = `📊 ${displayName} 流量分析`;
        }
        
        // 요약 정보 업데이트
        document.getElementById('source-detail-active-users').textContent = data.activeUsers.toLocaleString();
        const totalPageViews = data.pageViews.reduce((sum, item) => sum + item.pageViews, 0);
        document.getElementById('source-detail-total-pageviews').textContent = Math.round(totalPageViews).toLocaleString();
        
        // 전체 데이터 저장
        window.sourceDetailData = data;
        
        // 전체 제목 탭 표시
        showSubTab('source-detail', 'full');
        
        // 테이블 데이터 렌더링
        renderSourceDetailTables(data);
        
        // 카테고리 드롭다운 초기화 - wpCategoryData가 있을 때만
        if (window.wpCategoryData && Object.keys(window.wpCategoryData.categories || {}).length > 0) {
            if (window.initializeCategoryDropdown) {
                window.initializeCategoryDropdown('source-detail', data.pageViews);
            }
            if (window.checkCategoryDataAvailability) {
                window.checkCategoryDataAvailability('source-detail');
            }
        } else {
            // 카테고리 데이터가 없으면 나중에 로드되면 초기화하도록 설정
            const checkInterval = setInterval(() => {
                if (window.wpCategoryData && Object.keys(window.wpCategoryData.categories || {}).length > 0) {
                    if (window.initializeCategoryDropdown) {
                        window.initializeCategoryDropdown('source-detail', data.pageViews);
                    }
                    if (window.checkCategoryDataAvailability) {
                        window.checkCategoryDataAvailability('source-detail');
                    }
                    clearInterval(checkInterval);
                }
            }, 500);
        }
        
    } catch (error) {
        console.error('Error fetching source detail:', error);
        alert('도메인별 상세 데이터를 불러오는 중 오류가 발생했습니다.');
    } finally {
        if (loadingDiv) loadingDiv.style.display = 'none';
    }
}

// 도메인 상세 페이지 닫기
function closeSourceDetail() {
    document.getElementById('source-detail').style.display = 'none';
    
    // 데이터 초기화
    window.sourceDetailData = null;
}

// 카테고리 드롭다운 업데이트 - 더 이상 사용하지 않음 (initializeCategoryDropdown 사용)

// 도메인별 상세 데이터 테이블 렌더링
function renderSourceDetailTables(data) {
    const pageViews = data.pageViews || [];
    const categoryViews = data.categoryViews || [];
    
    // 전체 제목 테이블 렌더링
    renderPageTitleTable(pageViews, 'source-detail-page-title-views');
    
    // 접두어 테이블 렌더링
    const prefixData = analyzePrefixes(pageViews);
    renderPrefixTable(prefixData.prefix1, 'source-detail-prefix1-views');
    renderPrefixTable(prefixData.prefix2, 'source-detail-prefix2-views');
    renderPrefixTable(prefixData.prefix3, 'source-detail-prefix3-views');
    
    // 카테고리별 테이블 렌더링
    if (categoryViews.length > 0) {
        document.getElementById('sync-banner-source-detail').style.display = 'none';
        renderCategoryTable(categoryViews, 'source-detail-category-views');
    }
}

// 페이지 제목 테이블 렌더링
function renderPageTitleTable(pageViews, tableId) {
    const tbody = document.getElementById(tableId);
    tbody.innerHTML = '';
    
    const totalViews = pageViews.reduce((sum, item) => sum + item.pageViews, 0);
    
    pageViews.forEach((item, index) => {
        const row = document.createElement('tr');
        const percentage = totalViews > 0 ? ((item.pageViews / totalViews) * 100).toFixed(1) : '0.0';
        
        row.innerHTML = `
            <td>${index + 1}</td>
            <td>${item.pageTitle}</td>
            <td>${Math.round(item.pageViews).toLocaleString()}</td>
            <td>${percentage}%</td>
        `;
        
        tbody.appendChild(row);
    });
}

// 접두어 분석
function analyzePrefixes(pageViews) {
    const prefix1Map = {};
    const prefix2Map = {};
    const prefix3Map = {};
    
    pageViews.forEach(item => {
        const title = item.pageTitle;
        const words = title.split(/\s+/);
        const views = item.pageViews;
        
        // 1단어 접두어
        if (words.length >= 1) {
            const prefix = words[0];
            prefix1Map[prefix] = (prefix1Map[prefix] || 0) + views;
        }
        
        // 2단어 접두어
        if (words.length >= 2) {
            const prefix = words.slice(0, 2).join(' ');
            prefix2Map[prefix] = (prefix2Map[prefix] || 0) + views;
        }
        
        // 3단어 접두어
        if (words.length >= 3) {
            const prefix = words.slice(0, 3).join(' ');
            prefix3Map[prefix] = (prefix3Map[prefix] || 0) + views;
        }
    });
    
    // 배열로 변환하고 정렬
    const sortPrefixes = (prefixMap) => {
        return Object.entries(prefixMap)
            .map(([prefix, views]) => ({ prefix, views }))
            .sort((a, b) => b.views - a.views)
            .slice(0, 50); // 상위 50개만
    };
    
    return {
        prefix1: sortPrefixes(prefix1Map),
        prefix2: sortPrefixes(prefix2Map),
        prefix3: sortPrefixes(prefix3Map)
    };
}

// 접두어 테이블 렌더링
function renderPrefixTable(prefixData, tableId) {
    const tbody = document.getElementById(tableId);
    tbody.innerHTML = '';
    
    const totalViews = prefixData.reduce((sum, item) => sum + item.views, 0);
    
    prefixData.forEach((item, index) => {
        const row = document.createElement('tr');
        const percentage = totalViews > 0 ? ((item.views / totalViews) * 100).toFixed(1) : '0.0';
        
        row.innerHTML = `
            <td>${index + 1}</td>
            <td>${item.prefix}</td>
            <td>${Math.round(item.views).toLocaleString()}</td>
            <td>${percentage}%</td>
        `;
        
        tbody.appendChild(row);
    });
}

// 카테고리 테이블 렌더링
function renderCategoryTable(categoryViews, tableId) {
    const tbody = document.getElementById(tableId);
    tbody.innerHTML = '';
    
    const totalViews = categoryViews.reduce((sum, item) => sum + item.pageViews, 0);
    
    categoryViews.forEach((item, index) => {
        const row = document.createElement('tr');
        const percentage = totalViews > 0 ? ((item.pageViews / totalViews) * 100).toFixed(1) : '0.0';
        
        row.innerHTML = `
            <td>${index + 1}</td>
            <td>${item.categoryName}</td>
            <td>${Math.round(item.pageViews).toLocaleString()}</td>
            <td>${percentage}%</td>
        `;
        
        tbody.appendChild(row);
    });
}

// 도메인별 상세 데이터 카테고리 필터링
function filterSourceDetailByCategory(categoryId) {
    if (!window.sourceDetailData) return;
    
    const pageViews = window.sourceDetailData.pageViews || [];
    
    // 필터링 안 함 (전체 카테고리 선택)
    if (!categoryId) {
        renderPageTitleTable(pageViews, 'source-detail-page-title-views');
        return;
    }
    
    // 필터링된 데이터 생성
    let filteredData = [];
    
    if (pageViews && window.wpCategoryData) {
        filteredData = pageViews.filter(item => {
            const postId = window.extractPostId ? window.extractPostId(item.pagePath) : null;
            if (!postId) return false;
            
            const postCategories = window.wpCategoryData.posts[postId];
            return postCategories && postCategories.includes(parseInt(categoryId));
        });
    }
    
    // 필터링된 데이터로 테이블 업데이트
    renderPageTitleTable(filteredData, 'source-detail-page-title-views');
}

// window 객체에 함수 등록
window.showSourceDetail = showSourceDetail;
window.closeSourceDetail = closeSourceDetail;
window.filterSourceDetailByCategory = filterSourceDetailByCategory;