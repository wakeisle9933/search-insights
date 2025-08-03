// dashboard-search-query.js
// Search Console 유입 쿼리 분석 기능

// 선택된 날짜 정보를 저장
let searchQueryDateRange = {
    startDate: null,
    endDate: null
};

// 현재 선택된 서브 탭 (full, prefix1, prefix2, prefix3)
let currentSearchQuerySubTab = 'full';

// 조회한 원본 데이터 저장
let searchQueryRawData = null;

// 서브 탭 전환 함수
function showSearchQuerySubTab(tab) {
    // 모든 서브 탭 비활성화
    const tabs = document.querySelectorAll('#search-query-content .sub-tabs .sub-tab');
    tabs.forEach(t => t.classList.remove('active'));
    
    // 클릭한 탭 활성화
    event.target.classList.add('active');
    
    // 현재 탭 저장
    currentSearchQuerySubTab = tab;
    
    // 헤더 업데이트
    const isPrefix = tab !== 'full';
    updateSearchQueryTableHeaders(isPrefix);
    
    // 저장된 데이터가 있으면 클라이언트에서 처리
    if (searchQueryRawData) {
        processAndDisplayData();
    }
}

// 테이블 헤더 업데이트 함수
function updateSearchQueryTableHeaders(isPrefix) {
    const thead = document.querySelector('#search-query-content .dashboard-table thead tr');
    if (!thead) return;
    
    const headers = thead.querySelectorAll('th');
    if (headers.length > 6) {
        // 페이지 링크 헤더 표시/숨김
        headers[6].style.display = isPrefix ? 'none' : '';
    }
    
    // 검색어/접두어 헤더 텍스트 변경
    if (headers[1]) {
        headers[1].innerHTML = isPrefix ? 
            (window.t ? window.t('tableHeaders.prefix') : '접두어') : 
            (window.t ? window.t('tableHeaders.query') : '검색어');
    }
}

// 빠른 날짜 선택 기능
function setSearchQueryDateRange(days) {
    const today = new Date();
    // Google Search Console은 3일 지연이 있으므로 기본값은 3일 전
    const maxDate = new Date(today);
    maxDate.setDate(today.getDate() - 3);
    
    const endDate = new Date(maxDate);
    const startDate = new Date(maxDate);
    
    // 종료일에서 days-1 만큼 뺀 날짜가 시작일
    if (days > 1) {
        startDate.setDate(endDate.getDate() - days + 1);
    }
    
    // 날짜 입력 필드에 설정
    document.getElementById('search-query-start-date').value = formatDate(startDate);
    document.getElementById('search-query-end-date').value = formatDate(endDate);
    
    searchQueryDateRange.startDate = formatDate(startDate);
    searchQueryDateRange.endDate = formatDate(endDate);
}

// 날짜 유효성 검사
function validateSearchQueryDates(event) {
    const startDateInput = document.getElementById('search-query-start-date');
    const endDateInput = document.getElementById('search-query-end-date');
    const startDate = startDateInput.value;
    const endDate = endDateInput.value;
    
    if (!startDate || !endDate) {
        return;
    }
    
    const startDateObj = new Date(startDate);
    const endDateObj = new Date(endDate);
    
    // 3일 전 날짜 계산
    const today = new Date();
    const maxAllowedDate = new Date(today);
    maxAllowedDate.setDate(today.getDate() - 3);
    const maxDateStr = formatDate(maxAllowedDate);
    
    // 어떤 입력이 변경되었는지 확인
    const changedInput = event ? event.target : null;
    
    // 시작일이 변경된 경우
    if (changedInput === startDateInput) {
        // 시작일이 3일 전보다 최근인지 체크
        if (startDateObj > maxAllowedDate) {
            const message = window.t ? 
                window.t('errors.searchConsoleDelay').replace('{maxDate}', maxDateStr) : 
                `구글 Search Console 데이터는 3일의 지연이 있습니다.\n시작일은 ${maxDateStr} 이전이어야 합니다.`;
            alert(message);
            startDateInput.value = maxDateStr;
            searchQueryDateRange.startDate = maxDateStr;
            return;
        }
        
        // 시작일이 종료일보다 늦은지 체크
        if (startDateObj > endDateObj) {
            // 종료일을 시작일과 같게 설정
            endDateInput.value = startDate;
            searchQueryDateRange.endDate = startDate;
        }
    }
    // 종료일이 변경된 경우
    else if (changedInput === endDateInput) {
        // 종료일이 3일 전보다 최근인지 체크
        if (endDateObj > maxAllowedDate) {
            const message = window.t ? 
                window.t('errors.searchConsoleDelay').replace('{maxDate}', maxDateStr) : 
                `구글 Search Console 데이터는 3일의 지연이 있습니다.\n종료일은 ${maxDateStr} 이전이어야 합니다.`;
            alert(message);
            endDateInput.value = maxDateStr;
            searchQueryDateRange.endDate = maxDateStr;
            return;
        }
        
        // 종료일이 시작일보다 이른지 체크
        if (endDateObj < startDateObj) {
            // 시작일을 종료일과 같게 설정
            startDateInput.value = endDate;
            searchQueryDateRange.startDate = endDate;
        }
    }
    
    searchQueryDateRange.startDate = startDateInput.value;
    searchQueryDateRange.endDate = endDateInput.value;
}

// 검색 쿼리 데이터 가져오기
async function fetchSearchQueries() {
    try {
        // 로딩 상태 표시
        showSearchQueryLoadingState();
        
        // API 호출 (항상 전체 데이터를 가져옴)
        const params = new URLSearchParams();
        if (searchQueryDateRange.startDate) {
            params.append('fromDate', searchQueryDateRange.startDate);
        }
        if (searchQueryDateRange.endDate) {
            params.append('toDate', searchQueryDateRange.endDate);
        }
        
        // 항상 전체 데이터를 가져옴 (wordCount=0)
        params.append('wordCount', 0);
        
        const response = await fetch(`/api/search-queries?${params}`);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        
        // 원본 데이터 저장
        searchQueryRawData = data;
        
        // 데이터 처리 및 표시
        processAndDisplayData();
        
    } catch (error) {
        console.error('Error fetching search queries:', error);
        showSearchQueryErrorState();
    }
}

// 로딩 상태 표시
function showSearchQueryLoadingState() {
    const tbody = document.getElementById('search-query-table');
    const isPrefix = currentSearchQuerySubTab !== 'full';
    const colspan = isPrefix ? 6 : 7;
    
    tbody.innerHTML = `
        <tr>
            <td colspan="${colspan}" style="text-align: center; padding: 40px;">
                <div class="loading-spinner"></div>
                <div>${window.t ? window.t('messages.loadingData') : '데이터를 불러오는 중...'}</div>
            </td>
        </tr>
    `;
}

// 에러 상태 표시
function showSearchQueryErrorState() {
    const tbody = document.getElementById('search-query-table');
    const isPrefix = currentSearchQuerySubTab !== 'full';
    const colspan = isPrefix ? 6 : 7;
    
    tbody.innerHTML = `
        <tr>
            <td colspan="${colspan}" style="text-align: center; padding: 40px; color: #e74c3c;">
                ${window.t ? window.t('messages.dataLoadError') : '데이터를 불러오는 중 오류가 발생했습니다.'}
            </td>
        </tr>
    `;
}

// 검색 쿼리 데이터 렌더링
function renderSearchQueryData(data) {
    const tbody = document.getElementById('search-query-table');
    const isPrefix = currentSearchQuerySubTab !== 'full';
    const colspan = isPrefix ? 6 : 7;
    
    if (!data || data.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="${colspan}" style="text-align: center; padding: 40px;">
                    ${window.t ? window.t('messages.noData') : '해당 기간에 데이터가 없습니다.'}
                </td>
            </tr>
        `;
        return;
    }
    
    // 헤더 업데이트 (접두어 분석일 때는 페이지 링크 열 제거)
    updateSearchQueryTableHeaders(isPrefix);
    
    // 테이블 HTML 생성
    const html = data.map((item, index) => {
        const rank = index + 1;
        const truncatedLink = item.pageLink && item.pageLink.length > 50 ? item.pageLink.substring(0, 50) + '...' : item.pageLink;
        
        return `
            <tr>
                <td>${rank}</td>
                <td>
                    <div style="max-width: 200px; overflow-x: auto; white-space: nowrap; scrollbar-width: thin;" title="${escapeHtml(item.query)}">
                        ${escapeHtml(item.query)}
                    </div>
                </td>
                <td>${item.position.toFixed(1)}</td>
                <td>${formatNumber(item.clicks)}</td>
                <td>${formatNumber(item.impressions)}</td>
                <td>${item.ctr.toFixed(1)}%</td>
                ${!isPrefix ? `<td>
                    <a href="${escapeHtml(item.pageLink)}" target="_blank" title="${escapeHtml(item.pageLink)}" 
                       style="color: #0891b2; text-decoration: none;">
                        ${escapeHtml(truncatedLink)}
                    </a>
                </td>` : ''}
            </tr>
        `;
    }).join('');
    
    tbody.innerHTML = html;
}

// 요약 정보 표시
function showSearchQuerySummary(summary) {
    const summaryDiv = document.getElementById('search-query-summary');
    
    if (!summary || summary.totalQueries === 0) {
        summaryDiv.style.display = 'none';
        return;
    }
    
    // 요약 정보 표시 (서버에서 계산된 전체 데이터 기준)
    document.getElementById('total-queries').textContent = formatNumber(summary.totalQueries);
    document.getElementById('total-clicks').textContent = formatNumber(summary.totalClicks);
    document.getElementById('total-impressions').textContent = formatNumber(summary.totalImpressions);
    document.getElementById('avg-position').textContent = summary.avgPosition.toFixed(1);
    
    summaryDiv.style.display = 'flex';
}

// 날짜 포맷팅 함수
function formatDate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

// 숫자 포맷팅 함수 (천 단위 콤마)
function formatNumber(num) {
    return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
}

// HTML 이스케이프 함수
function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

// 데이터 처리 및 표시 함수
function processAndDisplayData() {
    if (!searchQueryRawData) return;
    
    // 요약 정보는 항상 전체 데이터 기준으로 표시
    showSearchQuerySummary(searchQueryRawData.summary);
    
    if (currentSearchQuerySubTab === 'full') {
        // 전체 쿼리: 상위 1000개만 표시 (성능을 위해)
        const displayData = searchQueryRawData.queries.slice(0, 1000);
        renderSearchQueryData(displayData);
    } else {
        // 접두어 분석: 클라이언트에서 그룹화
        const wordCount = currentSearchQuerySubTab === 'prefix1' ? 1 :
                         currentSearchQuerySubTab === 'prefix2' ? 2 : 3;
        
        const groupedData = groupByPrefix(searchQueryRawData.queries, wordCount);
        const prefixData = calculatePrefixSummary(groupedData);
        
        // 접두어 요약 정보를 테이블 형식으로 변환
        const formattedData = prefixData.map(item => ({
            query: item.prefix,
            position: item.avgPosition,
            clicks: item.totalClicks,
            impressions: item.totalImpressions,
            ctr: item.avgCTR,
            pageLink: ""  // 접두어 분석에는 pageLink가 없음
        }));
        
        // 상위 1000개만 표시 (성능을 위해)
        const displayData = formattedData.slice(0, 1000);
        
        // 데이터 렌더링
        renderSearchQueryData(displayData);
    }
}

// 클라이언트에서 접두어로 그룹화 (SpreadSheetService와 동일한 로직)
function groupByPrefix(queries, wordCount) {
    const grouped = {};
    
    queries.forEach(query => {
        // query가 객체인지 문자열인지 확인
        const queryText = typeof query === 'string' ? query : (query.query || '');
        const words = queryText.toLowerCase().split(/\s+/);
        
        let prefix;
        if (wordCount === 1) {
            // 1단어: 모든 쿼리 포함
            prefix = words[0] || '';
        } else if (words.length >= wordCount) {
            // 2단어 이상: 단어 수가 충분한 경우만 포함
            prefix = words.slice(0, wordCount).join(' ');
        } else {
            // 단어 수가 부족하면 제외
            return;
        }
        
        if (prefix) {
            if (!grouped[prefix]) {
                grouped[prefix] = [];
            }
            grouped[prefix].push(query);
        }
    });
    
    return grouped;
}

// 접두어별 요약 계산
function calculatePrefixSummary(groupedData) {
    const summaries = [];
    
    for (const [prefix, queries] of Object.entries(groupedData)) {
        const totalClicks = queries.reduce((sum, q) => sum + q.clicks, 0);
        const totalImpressions = queries.reduce((sum, q) => sum + q.impressions, 0);
        const avgPosition = queries.reduce((sum, q) => sum + q.position, 0) / queries.length;
        const avgCTR = totalImpressions > 0 ? (totalClicks / totalImpressions) * 100 : 0;
        
        summaries.push({
            prefix: prefix,
            avgPosition: avgPosition,
            totalClicks: totalClicks,
            totalImpressions: totalImpressions,
            avgCTR: avgCTR
        });
    }
    
    // impressions 기준 내림차순 정렬
    return summaries.sort((a, b) => b.totalImpressions - a.totalImpressions);
}

// 탭 초기화 함수
function initSearchQueryTab() {
    // 기본값으로 가장 최근 (3일 전) 설정
    setSearchQueryDateRange(1);
    
    // 초기 헤더 설정 (전체 쿼리 모드)
    updateSearchQueryTableHeaders(false);
}

// window 객체에 함수들 노출
window.setSearchQueryDateRange = setSearchQueryDateRange;
window.validateSearchQueryDates = validateSearchQueryDates;
window.fetchSearchQueries = fetchSearchQueries;
window.initSearchQueryTab = initSearchQueryTab;
window.showSearchQuerySubTab = showSearchQuerySubTab;

// 페이지 로드 시 날짜 필드 초기화 (탭이 보이지 않아도 설정)
document.addEventListener('DOMContentLoaded', function() {
    setTimeout(function() {
        const startDateInput = document.getElementById('search-query-start-date');
        const endDateInput = document.getElementById('search-query-end-date');
        
        if (startDateInput && endDateInput && !startDateInput.value && !endDateInput.value) {
            const today = new Date();
            const maxDate = new Date(today);
            maxDate.setDate(today.getDate() - 3);
            
            const dateStr = formatDate(maxDate);
            
            // 시작일과 종료일 모두 3일 전으로 설정 (가장 최근 1일)
            startDateInput.value = dateStr;
            endDateInput.value = dateStr;
            
            searchQueryDateRange.startDate = dateStr;
            searchQueryDateRange.endDate = dateStr;
        }
    }, 100);
});