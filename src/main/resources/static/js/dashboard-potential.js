// dashboard-potential.js
// Potential Hits 분석 기능 (Position > 3.0이면서 Impressions가 높은 쿼리)

// 선택된 날짜 정보를 저장
let potentialDateRange = {
    startDate: null,
    endDate: null
};

// 조회한 원본 데이터 저장
let potentialRawData = null;

// 빠른 날짜 선택 기능
function setPotentialDateRange(days) {
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
    document.getElementById('potential-start-date').value = formatDate(startDate);
    document.getElementById('potential-end-date').value = formatDate(endDate);
    
    potentialDateRange.startDate = formatDate(startDate);
    potentialDateRange.endDate = formatDate(endDate);
}

// 날짜 유효성 검사
function validatePotentialDates(event) {
    const startDateInput = document.getElementById('potential-start-date');
    const endDateInput = document.getElementById('potential-end-date');
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
            potentialDateRange.startDate = maxDateStr;
            return;
        }
        
        // 시작일이 종료일보다 늦은지 체크
        if (startDateObj > endDateObj) {
            // 종료일을 시작일과 같게 설정
            endDateInput.value = startDate;
            potentialDateRange.endDate = startDate;
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
            potentialDateRange.endDate = maxDateStr;
            return;
        }
        
        // 종료일이 시작일보다 이른지 체크
        if (endDateObj < startDateObj) {
            // 시작일을 종료일과 같게 설정
            startDateInput.value = endDate;
            potentialDateRange.startDate = endDate;
        }
    }
    
    potentialDateRange.startDate = startDateInput.value;
    potentialDateRange.endDate = endDateInput.value;
}

// 포텐셜 히트 데이터 가져오기
async function fetchPotentialHits() {
    try {
        // 로딩 상태 표시
        showPotentialLoadingState();
        
        // API 호출 (전체 데이터를 가져와서 클라이언트에서 필터링)
        const params = new URLSearchParams();
        if (potentialDateRange.startDate) {
            params.append('fromDate', potentialDateRange.startDate);
        }
        if (potentialDateRange.endDate) {
            params.append('toDate', potentialDateRange.endDate);
        }
        
        // 전체 데이터를 가져옴
        params.append('wordCount', 0);
        
        const response = await fetch(`/api/search-queries?${params}`);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        
        // 원본 데이터 저장
        potentialRawData = data;
        
        // 포텐셜 히트 필터링 및 표시
        filterAndDisplayPotentialHits();
        
    } catch (error) {
        console.error('Error fetching potential hits:', error);
        showPotentialErrorState();
    }
}

// 포텐셜 히트 필터링 및 표시
function filterAndDisplayPotentialHits() {
    if (!potentialRawData || !potentialRawData.queries) {
        showPotentialErrorState();
        return;
    }
    
    // Position > 3.0인 쿼리만 필터링
    const filteredQueries = potentialRawData.queries
        .filter(query => query.position > 3.0)
        .sort((a, b) => b.impressions - a.impressions);
    
    // 중복 제거: 같은 쿼리는 impressions가 가장 높은 것만 유지
    const uniqueQueries = [];
    const seenQueries = new Set();
    
    filteredQueries.forEach(query => {
        if (!seenQueries.has(query.query)) {
            uniqueQueries.push(query);
            seenQueries.add(query.query);
        }
    });
    
    // 요약 정보 계산
    const summary = {
        totalQueries: uniqueQueries.length,
        totalClicks: uniqueQueries.reduce((sum, q) => sum + q.clicks, 0),
        totalImpressions: uniqueQueries.reduce((sum, q) => sum + q.impressions, 0),
        avgPosition: uniqueQueries.length > 0 ? 
            uniqueQueries.reduce((sum, q) => sum + q.position, 0) / uniqueQueries.length : 0
    };
    
    // 요약 정보 표시
    showPotentialSummary(summary);
    
    // 테이블에 데이터 렌더링 (상위 1000개만)
    const displayData = uniqueQueries.slice(0, 1000);
    renderPotentialData(displayData);
}

// 로딩 상태 표시
function showPotentialLoadingState() {
    const tbody = document.getElementById('potential-table');
    tbody.innerHTML = `
        <tr>
            <td colspan="7" style="text-align: center; padding: 40px;">
                <div class="loading-spinner"></div>
                <div>${window.t ? window.t('messages.loadingData') : '데이터를 불러오는 중...'}</div>
            </td>
        </tr>
    `;
}

// 에러 상태 표시
function showPotentialErrorState() {
    const tbody = document.getElementById('potential-table');
    tbody.innerHTML = `
        <tr>
            <td colspan="7" style="text-align: center; padding: 40px; color: #e74c3c;">
                ${window.t ? window.t('messages.dataLoadError') : '데이터를 불러오는 중 오류가 발생했습니다.'}
            </td>
        </tr>
    `;
}

// 포텐셜 히트 데이터 렌더링
function renderPotentialData(data) {
    const tbody = document.getElementById('potential-table');
    
    if (!data || data.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" style="text-align: center; padding: 40px;">
                    ${window.t ? window.t('messages.noPotentialData') : '해당 기간에 포텐셜 히트 데이터가 없습니다.'}
                </td>
            </tr>
        `;
        return;
    }
    
    // 테이블 HTML 생성
    const html = data.map((item, index) => {
        const rank = index + 1;
        const truncatedLink = item.pageLink && item.pageLink.length > 50 ? 
            item.pageLink.substring(0, 50) + '...' : item.pageLink;
        
        // CTR 계산 (백분율)
        const ctr = item.impressions > 0 ? (item.clicks / item.impressions * 100) : 0;
        
        return `
            <tr>
                <td>${rank}</td>
                <td>
                    <div style="max-width: 200px; overflow-x: auto; white-space: nowrap; scrollbar-width: thin;" 
                         title="${escapeHtml(item.query)}">
                        ${escapeHtml(item.query)}
                    </div>
                </td>
                <td>${item.position.toFixed(1)}</td>
                <td>${formatNumber(item.clicks)}</td>
                <td>${formatNumber(item.impressions)}</td>
                <td>${ctr.toFixed(1)}%</td>
                <td>
                    <a href="${escapeHtml(item.pageLink)}" target="_blank" 
                       title="${escapeHtml(item.pageLink)}" 
                       style="color: #0891b2; text-decoration: none;">
                        ${escapeHtml(truncatedLink)}
                    </a>
                </td>
            </tr>
        `;
    }).join('');
    
    tbody.innerHTML = html;
}

// 요약 정보 표시
function showPotentialSummary(summary) {
    const summaryDiv = document.getElementById('potential-summary');
    
    if (!summary || summary.totalQueries === 0) {
        summaryDiv.style.display = 'none';
        return;
    }
    
    // 요약 정보 표시
    document.getElementById('total-potential').textContent = formatNumber(summary.totalQueries);
    document.getElementById('total-potential-clicks').textContent = formatNumber(summary.totalClicks);
    document.getElementById('total-potential-impressions').textContent = formatNumber(summary.totalImpressions);
    document.getElementById('avg-potential-position').textContent = summary.avgPosition.toFixed(1);
    
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

// 탭 초기화 함수
function initPotentialTab() {
    // 기본값으로 가장 최근 (3일 전) 설정
    setPotentialDateRange(1);
}

// window 객체에 함수들 노출
window.setPotentialDateRange = setPotentialDateRange;
window.validatePotentialDates = validatePotentialDates;
window.fetchPotentialHits = fetchPotentialHits;
window.initPotentialTab = initPotentialTab;

// 페이지 로드 시 날짜 필드 초기화 (탭이 보이지 않아도 설정)
document.addEventListener('DOMContentLoaded', function() {
    setTimeout(function() {
        const startDateInput = document.getElementById('potential-start-date');
        const endDateInput = document.getElementById('potential-end-date');
        
        if (startDateInput && endDateInput && !startDateInput.value && !endDateInput.value) {
            const today = new Date();
            const maxDate = new Date(today);
            maxDate.setDate(today.getDate() - 3);
            
            const dateStr = formatDate(maxDate);
            
            // 시작일과 종료일 모두 3일 전으로 설정 (가장 최근 1일)
            startDateInput.value = dateStr;
            endDateInput.value = dateStr;
            
            potentialDateRange.startDate = dateStr;
            potentialDateRange.endDate = dateStr;
        }
    }, 100);
});