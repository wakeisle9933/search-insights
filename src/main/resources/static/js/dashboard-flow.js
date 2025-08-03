// 플로우 분석 관련 전역 변수
let flowPageData = null;
let currentPageFlow = null;
let currentFlowData = null;  // 현재 표시중인 플로우 상세 데이터

// 플로우 탭 초기화
function initFlowTab() {
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
    
    document.getElementById('flow-start-date').value = formatDate(startDate);
    document.getElementById('flow-end-date').value = formatDate(endDate);
}

// 플로우 날짜 유효성 검증
function validateFlowDates() {
    const startDate = document.getElementById('flow-start-date').value;
    const endDate = document.getElementById('flow-end-date').value;
    
    if (startDate && endDate && startDate > endDate) {
        const message = window.t ? window.t('messages.invalidDateRange') : '시작일이 종료일보다 늦을 수 없어요!';
        alert(message);
        document.getElementById('flow-end-date').value = startDate;
    }
}

// 플로우 빠른 날짜 설정
function setFlowDateRange(days) {
    const excludeToday = document.getElementById('flow-exclude-today').checked;
    const now = new Date();
    const endDate = new Date(now);
    
    if (excludeToday) {
        endDate.setDate(endDate.getDate() - 1); // 어제를 종료일로
    }
    
    const startDate = new Date(endDate);
    startDate.setDate(startDate.getDate() - (days - 1));
    
    const formatDate = (date) => {
        return date.getFullYear() + '-' +
            String(date.getMonth() + 1).padStart(2, '0') + '-' +
            String(date.getDate()).padStart(2, '0');
    };
    
    document.getElementById('flow-start-date').value = formatDate(startDate);
    document.getElementById('flow-end-date').value = formatDate(endDate);
}

// 플로우 데이터 가져오기
async function fetchFlowData() {
    const startDate = document.getElementById('flow-start-date').value;
    const endDate = document.getElementById('flow-end-date').value;
    
    if (!startDate || !endDate) {
        const message = window.t ? window.t('messages.selectDates') : '시작일과 종료일을 모두 선택해주세요!';
        alert(message);
        return;
    }
    
    // UI 상태 변경
    document.getElementById('flow-initial-message').style.display = 'none';
    document.getElementById('flow-loading').style.display = 'block';
    document.getElementById('flow-data-container').style.display = 'none';
    
    try {
        // 상위 페이지 데이터 가져오기 (기존 API 활용)
        const response = await fetch(`/api/custom-date-pageviews?startDate=${startDate}&endDate=${endDate}`);
        if (!response.ok) {
            throw new Error('Failed to fetch flow data');
        }
        
        const data = await response.json();
        flowPageData = data.pageViews;
        
        // 상위 20개만 표시
        renderFlowPages(flowPageData.slice(0, 20));
        
        // 업데이트 시간 갱신
        updateFlowTime();
        
        // UI 상태 변경
        document.getElementById('flow-loading').style.display = 'none';
        document.getElementById('flow-data-container').style.display = 'block';
    } catch (error) {
        console.error('Error fetching flow data:', error);
        const errorMessage = window.t ? window.t('errors.loadDetailFailed') : '플로우 데이터를 가져오는 중 오류가 발생했습니다.';
        alert(errorMessage);
        document.getElementById('flow-loading').style.display = 'none';
        document.getElementById('flow-initial-message').style.display = 'block';
    }
}

// 플로우 페이지 목록 렌더링
function renderFlowPages(data) {
    const tbody = document.getElementById('flow-pages-table');
    tbody.innerHTML = ''; // 기존 데이터 초기화
    
    // 현재 언어에 맞는 로케일 설정
    const currentLang = window.getCurrentLanguage ? window.getCurrentLanguage() : 'ko';
    const locale = currentLang === 'ko' ? 'ko-KR' : 
                   currentLang === 'en' ? 'en-US' : 'zh-CN';
    
    if (!data || data.length === 0) {
        const noDataMessage = window.t ? window.t('messages.noData') : '데이터가 없어요!';
        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="flow-no-data-cell">
                    ${noDataMessage}
                </td>
            </tr>
        `;
        return;
    }
    
    // 데이터 렌더링
    data.forEach((item, index) => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${index + 1}</td>
            <td class="flow-table-cell-ellipsis" title="${item.pageTitle || window.t('messages.noTitle')}">
                ${item.pageTitle || window.t('messages.noTitle')}
            </td>
            <td class="flow-table-cell-ellipsis" title="${item.pagePath}">
                ${item.pagePath}
            </td>
            <td>${item.pageViews.toLocaleString(locale)}</td>
            <td>
                <button onclick="showPageFlow('${escapeHtml(item.pagePath)}', '${escapeHtml(item.pageTitle || window.t('messages.noTitle'))}')" 
                        class="flow-analyze-btn">
                    🔍 ${window.t('flowDetail.analyzeFlow')}
                </button>
            </td>
        `;
        tbody.appendChild(row);
    });
}

// 페이지 필터링
function filterFlowPages() {
    const searchText = document.getElementById('flow-page-search').value.toLowerCase();
    
    if (!flowPageData) return;
    
    const filteredData = flowPageData.filter(item => {
        const title = (item.pageTitle || '').toLowerCase();
        const path = (item.pagePath || '').toLowerCase();
        return title.includes(searchText) || path.includes(searchText);
    });
    
    renderFlowPages(filteredData.slice(0, 20));
}

// 플로우 업데이트 시간 갱신
function updateFlowTime() {
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
    
    const lastUpdateText = window.t ? window.t('labels.lastUpdate') : '마지막 업데이트';
    const timeElement = document.getElementById('flow-update-time');
    if (timeElement) {
        timeElement.innerHTML = `<span data-i18n="labels.lastUpdate">${lastUpdateText}</span>: ${timeString}`;
    }
}

// 페이지 플로우 상세 보기
async function showPageFlow(pagePath, pageTitle) {
    const detailBox = document.getElementById('flow-detail');
    const detailContent = document.getElementById('flow-detail-content');
    
    // 로딩 표시
    detailContent.innerHTML = `
        <div class="flow-loading-container">
            <div class="loading-spinner"></div>
            <div class="flow-loading-text">${window.t('flowDetail.analyzingFlow')}</div>
        </div>
    `;
    detailBox.style.display = 'block';
    
    // 스크롤
    detailBox.scrollIntoView({ behavior: 'smooth', block: 'center' });
    
    try {
        const startDate = document.getElementById('flow-start-date').value;
        const endDate = document.getElementById('flow-end-date').value;
        
        // 실제 플로우 API 호출
        const response = await fetch(`/api/page-flow?pagePath=${encodeURIComponent(pagePath)}&startDate=${startDate}&endDate=${endDate}`);
        
        if (!response.ok) {
            throw new Error('Failed to fetch page flow data');
        }
        
        const flowData = await response.json();
        currentFlowData = flowData;  // 전역 변수에 저장
        renderFlowDetail(flowData);
    } catch (error) {
        console.error('Error fetching page flow:', error);
        detailContent.innerHTML = `
            <div class="flow-error-container">
                ${window.t ? window.t('errors.loadDetailFailed') : '플로우 데이터를 가져오는 중 오류가 발생했습니다.'}
            </div>
        `;
    }
}

// 소스 페이지 표시 타이틀 결정 헬퍼 함수
function getSourceDisplayTitle(source, currentLang, directEntryText) {
    // sourceTitle이 있고 빈 문자열이 아니면 사용
    if (source.sourceTitle && source.sourceTitle.trim()) {
        return source.sourceTitle;
    }
    
    // 직접 유입인 경우
    if (source.sourcePage === '직접 유입') {
        return directEntryText[currentLang];
    }
    
    // 외부 사이트인 경우 URL에서 도메인만 추출해서 표시
    if (source.isExternal && source.sourcePage) {
        try {
            const url = new URL(source.sourcePage);
            return url.hostname;
        } catch (e) {
            // URL 파싱 실패 시 전체 URL 표시
            return source.sourcePage;
        }
    }
    
    // sourcePage가 있고 빈 문자열이 아니면 사용
    if (source.sourcePage && source.sourcePage.trim()) {
        return source.sourcePage;
    }
    
    // 모두 비어있으면 기본값
    return window.t ? window.t('messages.unknownPage') : 'Unknown Page';
}

// 소스 페이지 경로 표시 여부 결정
function shouldShowSourcePath(source) {
    // 외부 사이트의 경우 전체 URL을 표시
    if (source.isExternal && source.sourcePage) {
        return true;
    }
    
    // 내부 페이지: sourceTitle이 있고, sourcePage와 다르고, 둘 다 빈 문자열이 아닐 때만 표시
    return source.sourceTitle && 
           source.sourceTitle.trim() && 
           source.sourcePage && 
           source.sourcePage.trim() && 
           source.sourcePage !== source.sourceTitle;
}

// 소스 페이지 경로 표시 텍스트 결정
function getSourceDisplayPath(source, currentLang, directEntryText) {
    if (source.sourcePage === '직접 유입') {
        return directEntryText[currentLang];
    }
    return source.sourcePage || '';
}

// 플로우 상세 렌더링
function renderFlowDetail(flowData) {
    const detailContent = document.getElementById('flow-detail-content');
    
    // 현재 언어에 맞는 로케일 설정
    const currentLang = window.getCurrentLanguage ? window.getCurrentLanguage() : 'ko';
    const locale = currentLang === 'ko' ? 'ko-KR' : 
                   currentLang === 'en' ? 'en-US' : 'zh-CN'
    
    // 직접 유입 텍스트 (언어별)
    const directEntryText = {
        ko: '직접 유입',
        en: 'Direct Entry',
        zh: '直接进入'
    };
    
    // 아이콘 매핑
    const getSourceIcon = (source) => {
        if (source.isExternal) {
            if (source.sourcePage.includes('google')) return '🔍';
            if (source.sourcePage.includes('naver')) return '🟨';
            return '🌐';
        } else if (source.sourcePage === directEntryText[currentLang] || source.sourcePage === '직접 유입') {
            return '🔗';
        } else if (source.sourcePage === '/') {
            return '🏠';
        }
        return '📄';
    };
    
    const getDestinationIcon = (dest) => {
        if (dest.isExit) return '🚪';
        if (dest.destinationPage === '/') return '🏠';
        return '📄';
    };
    
    detailContent.innerHTML = `
        <div class="flow-detail-wrapper">
            <!-- 페이지 정보 -->
            <div class="flow-page-info">
                <h3 class="flow-page-title">📄 ${flowData.pageTitle}</h3>
                <div class="flow-page-path">${flowData.pagePath}</div>
                <div class="flow-page-views">
                    ${window.t('flowDetail.totalViews')}: ${flowData.totalViews.toLocaleString(locale)}
                </div>
            </div>
            
            <!-- 어디서 왔나? / 어디로 갔나? -->
            <div class="dashboard-box">
                <div class="dashboard-title">🌊 ${window.t('flowDetail.userFlowAnalysis')}</div>
                <div class="flow-grid-container">
                    <!-- 유입 경로 -->
                    <div class="flow-column">
                        <h4 class="flow-section-title">🔙 ${window.t('flowDetail.previousPage')}</h4>
                        <div class="flow-section-box">
                        ${flowData.sources.length > 0 ? flowData.sources.slice(0, 10).map(source => `
                            <div class="flow-item">
                                <span class="flow-icon">${getSourceIcon(source)}</span>
                                <div class="flow-content">
                                    <div class="flow-content-title" title="${getSourceDisplayTitle(source, currentLang, directEntryText)}">
                                        ${getSourceDisplayTitle(source, currentLang, directEntryText)}
                                    </div>
                                    ${shouldShowSourcePath(source) ? 
                                        `<div class="flow-content-path" title="${getSourceDisplayPath(source, currentLang, directEntryText)}">${getSourceDisplayPath(source, currentLang, directEntryText)}</div>` : ''}
                                    <div class="flow-progress-bar">
                                        <div class="flow-progress-source" style="width: ${Math.min(source.percentage.toFixed(1), 100)}%;"></div>
                                    </div>
                                </div>
                                <span class="flow-percentage flow-percentage-source">${source.percentage.toFixed(1)}%</span>
                            </div>
                        `).join('') : `<div class="flow-no-data">${window.t('messages.noData')}</div>`}
                    </div>
                </div>
                
                <!-- 이탈 경로 -->
                <div class="flow-column">
                    <h4 class="flow-section-title">🔜 ${window.t('flowDetail.nextPage')}</h4>
                    <div class="flow-section-box">
                        ${flowData.destinations.length > 0 ? flowData.destinations.slice(0, 10).map(dest => `
                            <div class="flow-item">
                                <span class="flow-icon">${getDestinationIcon(dest)}</span>
                                <div class="flow-content">
                                    <div class="flow-content-title" title="${dest.isExit ? window.t('flowDetail.siteExit') : (dest.destinationTitle || dest.destinationPage)}">
                                        ${dest.isExit ? window.t('flowDetail.siteExit') : (dest.destinationTitle || dest.destinationPage)}
                                    </div>
                                    ${dest.destinationTitle && dest.destinationPage !== dest.destinationTitle && !dest.isExit ? 
                                        `<div class="flow-content-path" title="${dest.destinationPage}">${dest.destinationPage}</div>` : ''}
                                    <div class="flow-progress-bar">
                                        <div class="flow-progress-destination" style="width: ${Math.min(dest.percentage.toFixed(1), 100)}%;"></div>
                                    </div>
                                </div>
                                <span class="flow-percentage flow-percentage-destination">${dest.percentage.toFixed(1)}%</span>
                            </div>
                        `).join('') : `<div class="flow-no-data">${window.t('messages.noData')}</div>`}
                    </div>
                </div>
            </div>
            </div>
            
            ${!flowData.sources.length && !flowData.destinations.length ? `
                <!-- 데이터 없음 안내 -->
                <div class="dashboard-box flow-no-data-box">
                <div class="flow-no-data-alert">
                    <div class="flow-no-data-title">
                        ⚠️ ${window.t('flowDetail.noFlowData')}
                    </div>
                    <div class="flow-no-data-description">
                        ${window.t('flowDetail.noFlowDataDesc')}<br>
                        ${window.t('flowDetail.noFlowDataSuggestion')}
                    </div>
                </div>
                </div>
            ` : ''}
        </div>
    `;
}

// 플로우 상세 닫기
function closeFlowDetail() {
    document.getElementById('flow-detail').style.display = 'none';
    // currentFlowData는 유지 (언어 변경 시 재사용을 위해)
}

// HTML 이스케이프
function escapeHtml(text) {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, m => map[m]);
}

// window 객체에 함수 등록
window.initFlowTab = initFlowTab;
window.validateFlowDates = validateFlowDates;
window.setFlowDateRange = setFlowDateRange;
window.fetchFlowData = fetchFlowData;
window.filterFlowPages = filterFlowPages;
window.showPageFlow = showPageFlow;
window.closeFlowDetail = closeFlowDetail;
window.renderFlowDetail = renderFlowDetail;
window.renderFlowPages = renderFlowPages;
window.updateFlowTime = updateFlowTime;
window.getSourceDisplayTitle = getSourceDisplayTitle;
window.shouldShowSourcePath = shouldShowSourcePath;
window.getSourceDisplayPath = getSourceDisplayPath;

// 전역 변수 getter 추가
Object.defineProperty(window, 'currentFlowData', {
    get: function() { return currentFlowData; },
    set: function(value) { currentFlowData = value; }
});

Object.defineProperty(window, 'flowPageData', {
    get: function() { return flowPageData; },
    set: function(value) { flowPageData = value; }
});