// 플로우 분석 관련 전역 변수
let flowPageData = null;
let currentPageFlow = null;

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
        const message = typeof t === 'function' ? t('messages.invalidDateRange') : '시작일이 종료일보다 늦을 수 없어요!';
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
        const message = typeof t === 'function' ? t('messages.selectDates') : '시작일과 종료일을 모두 선택해주세요!';
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
        alert('플로우 데이터를 가져오는 중 오류가 발생했습니다.');
        document.getElementById('flow-loading').style.display = 'none';
        document.getElementById('flow-initial-message').style.display = 'block';
    }
}

// 플로우 페이지 목록 렌더링
function renderFlowPages(data) {
    const tbody = document.getElementById('flow-pages-table');
    tbody.innerHTML = ''; // 기존 데이터 초기화
    
    if (!data || data.length === 0) {
        const noDataMessage = typeof t === 'function' ? t('messages.noData') : '데이터가 없어요!';
        tbody.innerHTML = `
            <tr>
                <td colspan="5" style="text-align: center; padding: 40px; color: #666;">
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
            <td style="max-width: 300px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${item.pageTitle || '(제목 없음)'}">
                ${item.pageTitle || '(제목 없음)'}
            </td>
            <td style="max-width: 300px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${item.pagePath}">
                ${item.pagePath}
            </td>
            <td>${item.pageViews.toLocaleString()}</td>
            <td>
                <button onclick="showPageFlow('${escapeHtml(item.pagePath)}', '${escapeHtml(item.pageTitle || '(제목 없음)')}')" 
                        class="analyze-btn" 
                        style="background: #9333ea; color: white; border: none; padding: 5px 15px; border-radius: 4px; cursor: pointer; font-size: 0.9em;">
                    🔍 ${t('flowDetail.analyzeFlow')}
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
    
    const lastUpdateText = typeof t === 'function' ? t('labels.lastUpdate') : '마지막 업데이트';
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
        <div style="text-align: center; padding: 40px;">
            <div class="loading-spinner"></div>
            <div style="margin-top: 20px;">${t('flowDetail.analyzingFlow')}</div>
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
        renderFlowDetail(flowData);
    } catch (error) {
        console.error('Error fetching page flow:', error);
        detailContent.innerHTML = `
            <div style="text-align: center; padding: 40px; color: #e74c3c;">
                플로우 데이터를 가져오는 중 오류가 발생했습니다.
            </div>
        `;
    }
}


// 플로우 상세 렌더링
function renderFlowDetail(flowData) {
    const detailContent = document.getElementById('flow-detail-content');
    
    // 아이콘 매핑
    const getSourceIcon = (source) => {
        if (source.isExternal) {
            if (source.sourcePage.includes('google')) return '🔍';
            if (source.sourcePage.includes('naver')) return '🟨';
            return '🌐';
        } else if (source.sourcePage === '직접 유입') {
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
        <div style="padding: 20px;">
            <!-- 페이지 정보 -->
            <div style="background: #f8f9fa; padding: 15px; border-radius: 8px; margin-bottom: 30px;">
                <h3 style="margin: 0 0 10px 0; color: #333;">📄 ${flowData.pageTitle}</h3>
                <div style="color: #666; font-size: 0.9em;">${flowData.pagePath}</div>
                <div style="color: #9333ea; font-weight: bold; margin-top: 10px;">
                    ${t('flowDetail.totalViews')}: ${flowData.totalViews.toLocaleString()}
                </div>
            </div>
            
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 30px; max-width: 1200px; margin: 0 auto;">
                <!-- 유입 경로 -->
                <div style="min-width: 0;">
                    <h4 style="color: #333; margin-bottom: 15px;">🔙 ${t('flowDetail.previousPage')}</h4>
                    <div style="background: #f8f9fa; padding: 20px; border-radius: 8px; overflow: hidden;">
                        ${flowData.sources.length > 0 ? flowData.sources.slice(0, 10).map(source => `
                            <div style="display: flex; align-items: center; margin-bottom: 15px;">
                                <span style="font-size: 1.5em; margin-right: 10px; flex-shrink: 0;">${getSourceIcon(source)}</span>
                                <div style="flex: 1; min-width: 0;">
                                    <div style="font-weight: 600; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${source.sourceTitle || source.sourcePage}">
                                        ${source.sourceTitle || source.sourcePage}
                                    </div>
                                    ${source.sourceTitle && source.sourcePage !== source.sourceTitle ? 
                                        `<div style="font-size: 0.8em; color: #666; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${source.sourcePage}">${source.sourcePage}</div>` : ''}
                                    <div style="background: #e9ecef; height: 20px; border-radius: 10px; margin-top: 5px; overflow: hidden;">
                                        <div style="background: #9333ea; height: 100%; width: ${Math.min(source.percentage.toFixed(1), 100)}%; transition: width 0.5s ease;"></div>
                                    </div>
                                </div>
                                <span style="margin-left: 10px; font-weight: bold; color: #9333ea; flex-shrink: 0;">${source.percentage.toFixed(1)}%</span>
                            </div>
                        `).join('') : '<div style="color: #666;">데이터가 없습니다.</div>'}
                    </div>
                </div>
                
                <!-- 이탈 경로 -->
                <div style="min-width: 0;">
                    <h4 style="color: #333; margin-bottom: 15px;">🔜 ${t('flowDetail.nextPage')}</h4>
                    <div style="background: #f8f9fa; padding: 20px; border-radius: 8px; overflow: hidden;">
                        ${flowData.destinations.length > 0 ? flowData.destinations.slice(0, 10).map(dest => `
                            <div style="display: flex; align-items: center; margin-bottom: 15px;">
                                <span style="font-size: 1.5em; margin-right: 10px; flex-shrink: 0;">${getDestinationIcon(dest)}</span>
                                <div style="flex: 1; min-width: 0;">
                                    <div style="font-weight: 600; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${dest.isExit ? t('flowDetail.siteExit') : (dest.destinationTitle || dest.destinationPage)}">
                                        ${dest.isExit ? t('flowDetail.siteExit') : (dest.destinationTitle || dest.destinationPage)}
                                    </div>
                                    ${dest.destinationTitle && dest.destinationPage !== dest.destinationTitle && !dest.isExit ? 
                                        `<div style="font-size: 0.8em; color: #666; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${dest.destinationPage}">${dest.destinationPage}</div>` : ''}
                                    <div style="background: #e9ecef; height: 20px; border-radius: 10px; margin-top: 5px; overflow: hidden;">
                                        <div style="background: #6366f1; height: 100%; width: ${Math.min(dest.percentage.toFixed(1), 100)}%; transition: width 0.5s ease;"></div>
                                    </div>
                                </div>
                                <span style="margin-left: 10px; font-weight: bold; color: #6366f1; flex-shrink: 0;">${dest.percentage.toFixed(1)}%</span>
                            </div>
                        `).join('') : '<div style="color: #666;">데이터가 없습니다.</div>'}
                    </div>
                </div>
            </div>
            
            ${!flowData.sources.length && !flowData.destinations.length ? `
                <!-- 데이터 없음 안내 -->
                <div style="margin-top: 30px; padding: 20px; background: #f8d7da; border: 1px solid #f5c6cb; border-radius: 8px;">
                    <div style="color: #721c24; font-weight: bold; margin-bottom: 10px;">
                        ⚠️ 플로우 데이터가 없습니다
                    </div>
                    <div style="color: #721c24; font-size: 0.9em;">
                        선택한 기간 동안 이 페이지의 플로우 데이터가 충분하지 않습니다.<br>
                        더 긴 기간을 선택하거나, 다른 페이지를 확인해보세요.
                    </div>
                </div>
            ` : ''}
        </div>
    `;
}

// 플로우 상세 닫기
function closeFlowDetail() {
    document.getElementById('flow-detail').style.display = 'none';
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