// 시간대별 사용자 활동 히트맵 관련 함수들 🔥

let heatmapChart = null;
let heatmapData = null;
let hourlyDetailData = null;  // 시간대별 상세 데이터 저장

// 히트맵 데이터 가져오기
async function fetchHeatmapData() {
    const startDate = document.getElementById('chart-start-date').value;
    const endDate = document.getElementById('chart-end-date').value;
    
    if (!startDate || !endDate) {
        console.log('날짜를 선택해주세요!');
        return;
    }
    
    // 로딩 시작!! 🔄
    const container = document.getElementById('heatmap-container');
    const initialMessage = document.getElementById('heatmap-initial-message');
    const loadingIndicator = document.getElementById('heatmap-loading');
    
    if (initialMessage) initialMessage.style.display = 'none';
    if (loadingIndicator) loadingIndicator.style.display = 'block';
    
    try {
        const response = await fetch(`/api/hourly-heatmap?startDate=${startDate}&endDate=${endDate}`);
        if (!response.ok) throw new Error('히트맵 데이터 가져오기 실패');
        
        const data = await response.json();
        heatmapData = data;
        window.heatmapData = data; // window에도 저장
        renderHeatmap(data);
    } catch (error) {
        console.error('히트맵 데이터 오류:', error);
        showError('히트맵 데이터를 불러올 수 없습니다 😢');
    } finally {
        // 로딩 종료!! ✨
        if (loadingIndicator) loadingIndicator.style.display = 'none';
    }
}

// 히트맵 렌더링
function renderHeatmap(data) {
    const container = document.getElementById('heatmap-container');
    if (!container) return;
    
    // 초기 메시지 숨기기
    const initialMessage = document.getElementById('heatmap-initial-message');
    if (initialMessage) initialMessage.style.display = 'none';
    
    // 요일 라벨 (월요일부터 시작)
    const dayLabels = {
        'ko': ['월', '화', '수', '목', '금', '토', '일'],
        'en': ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
        'zh': ['一', '二', '三', '四', '五', '六', '日']
    };
    
    const currentLang = localStorage.getItem('language') || 'ko';
    const days = dayLabels[currentLang];
    
    // 히트맵 HTML 생성
    let html = `
        <div class="heatmap-wrapper">
            <h3 class="heatmap-title">${window.t ? window.t('heatmap.title') : '시간대별 활동'}</h3>
            <div class="heatmap-grid">
                <div class="heatmap-hours">
                    ${generateHourLabels()}
                </div>
                <div class="heatmap-days">
                    ${generateDayLabels(days)}
                </div>
                <div class="heatmap-cells">
                    ${generateHeatmapCells(data.heatmapData)}
                </div>
            </div>
            <div class="heatmap-legend">
                <span class="legend-label">${window.t ? window.t('heatmap.less') : '적음'}</span>
                <div class="legend-gradient"></div>
                <span class="legend-label">${window.t ? window.t('heatmap.more') : '많음'}</span>
            </div>
        </div>
    `;
    
    container.innerHTML = html;
    
    // 툴팁 이벤트 추가
    addHeatmapTooltips();
}

// 시간 라벨 생성
function generateHourLabels() {
    let labels = '';
    for (let i = 0; i < 24; i += 2) { // 2시간 간격으로 표시
        const hour = i === 0 ? '12am' : (i < 12 ? `${i}am` : (i === 12 ? '12pm' : `${i-12}pm`));
        labels += `<div class="hour-label">${hour}</div>`;
    }
    return labels;
}

// 요일 라벨 생성
function generateDayLabels(days) {
    return days.map(day => `<div class="day-label">${day}</div>`).join('');
}

// 히트맵 셀 생성
function generateHeatmapCells(data) {
    if (!data || data.length === 0) return '';
    
    // 최대값 찾기 (색상 스케일용)
    let maxValue = 0;
    data.forEach(row => {
        row.forEach(value => {
            if (value > maxValue) maxValue = value;
        });
    });
    
    let cells = '';
    for (let day = 0; day < 7; day++) {
        // 월요일부터 시작하도록 인덱스 조정 (일요일을 마지막으로)
        const dataIndex = day === 6 ? 0 : day + 1; // 월(1)~토(6) -> 0~5, 일(0) -> 6
        
        for (let hour = 0; hour < 24; hour++) {
            const value = data[dataIndex][hour];
            const intensity = maxValue > 0 ? (value / maxValue) : 0;
            const bgColor = getHeatmapColor(intensity);
            
            cells += `
                <div class="heatmap-cell" 
                     style="background-color: ${bgColor}; cursor: pointer;"
                     data-day="${dataIndex}" 
                     data-hour="${hour}" 
                     data-value="${value}"
                     onclick="showHourlyDetail(${dataIndex}, ${hour})">
                </div>
            `;
        }
    }
    
    return cells;
}

// 히트맵 색상 계산 (파란색 그라데이션)
function getHeatmapColor(intensity) {
    if (intensity === 0) return '#f5f5f5'; // 회색 (데이터 없음)
    
    // 더 극적인 색상 변화!! 🔥
    if (intensity < 0.2) {
        // 아주 연한 파란색 (거의 흰색)
        const minColor = { r: 232, g: 245, b: 253 };
        const midColor = { r: 179, g: 229, b: 252 };
        const localIntensity = intensity / 0.2;
        
        const r = Math.round(minColor.r + (midColor.r - minColor.r) * localIntensity);
        const g = Math.round(minColor.g + (midColor.g - minColor.g) * localIntensity);
        const b = Math.round(minColor.b + (midColor.b - minColor.b) * localIntensity);
        return `rgb(${r}, ${g}, ${b})`;
    } else if (intensity < 0.5) {
        // 연한 파란색에서 중간 파란색으로
        const midColor = { r: 179, g: 229, b: 252 };
        const medColor = { r: 66, g: 165, b: 245 };
        const localIntensity = (intensity - 0.2) / 0.3;
        
        const r = Math.round(midColor.r + (medColor.r - midColor.r) * localIntensity);
        const g = Math.round(midColor.g + (medColor.g - midColor.g) * localIntensity);
        const b = Math.round(midColor.b + (medColor.b - midColor.b) * localIntensity);
        return `rgb(${r}, ${g}, ${b})`;
    } else if (intensity < 0.8) {
        // 중간 파란색에서 진한 파란색으로
        const medColor = { r: 66, g: 165, b: 245 };
        const darkColor = { r: 13, g: 71, b: 161 };
        const localIntensity = (intensity - 0.5) / 0.3;
        
        const r = Math.round(medColor.r + (darkColor.r - medColor.r) * localIntensity);
        const g = Math.round(medColor.g + (darkColor.g - medColor.g) * localIntensity);
        const b = Math.round(medColor.b + (darkColor.b - medColor.b) * localIntensity);
        return `rgb(${r}, ${g}, ${b})`;
    } else {
        // 아주 진한 파란색 (거의 남색)
        const darkColor = { r: 13, g: 71, b: 161 };
        const maxColor = { r: 0, g: 31, b: 63 };
        const localIntensity = (intensity - 0.8) / 0.2;
        
        const r = Math.round(darkColor.r + (maxColor.r - darkColor.r) * localIntensity);
        const g = Math.round(darkColor.g + (maxColor.g - darkColor.g) * localIntensity);
        const b = Math.round(darkColor.b + (maxColor.b - darkColor.b) * localIntensity);
        return `rgb(${r}, ${g}, ${b})`;
    }
}

// 툴팁 추가
function addHeatmapTooltips() {
    const cells = document.querySelectorAll('.heatmap-cell');
    
    cells.forEach(cell => {
        cell.addEventListener('mouseenter', function(e) {
            const day = parseInt(this.dataset.day);
            const hour = parseInt(this.dataset.hour);
            const value = parseInt(this.dataset.value);
            
            const dayLabels = {
                'ko': ['일요일', '월요일', '화요일', '수요일', '목요일', '금요일', '토요일'],
                'en': ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'],
                'zh': ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
            };
            
            const currentLang = localStorage.getItem('language') || 'ko';
            const dayName = dayLabels[currentLang][day];
            const timeStr = `${hour}:00 - ${hour + 1}:00`;
            
            const tooltip = document.createElement('div');
            tooltip.className = 'heatmap-tooltip';
            tooltip.innerHTML = `
                <strong>${dayName}</strong><br>
                ${timeStr}<br>
                ${window.t ? window.t('heatmap.users') : '사용자'}: ${value}
            `;
            
            document.body.appendChild(tooltip);
            
            const rect = this.getBoundingClientRect();
            tooltip.style.left = rect.left + (rect.width / 2) - (tooltip.offsetWidth / 2) + 'px';
            tooltip.style.top = rect.top - tooltip.offsetHeight - 5 + 'px';
        });
        
        cell.addEventListener('mouseleave', function() {
            const tooltips = document.querySelectorAll('.heatmap-tooltip');
            tooltips.forEach(t => t.remove());
        });
    });
}

// 히트맵 새로고침 (일간 차트와 연동)
function refreshHeatmap() {
    if (document.getElementById('heatmap-container')) {
        fetchHeatmapData();
    }
}

// 에러 표시
function showError(message) {
    const container = document.getElementById('heatmap-container');
    if (container) {
        container.innerHTML = `<div class="error-message">${message}</div>`;
    }
}

// 시간대별 상세 데이터 표시
async function showHourlyDetail(dayOfWeek, hour) {
    // 다른 상세 페이지 닫기
    const dailyDetail = document.getElementById('daily-chart-detail');
    if (dailyDetail && dailyDetail.style.display !== 'none') {
        dailyDetail.style.display = 'none';
    }
    
    // 로딩 표시
    const loadingDiv = document.getElementById('hourly-detail-loading');
    if (loadingDiv) loadingDiv.style.display = 'block';
    
    // 차트의 날짜 범위에서 현재 날짜 계산
    const startDate = document.getElementById('chart-start-date').value;
    const endDate = document.getElementById('chart-end-date').value;
    
    if (!startDate || !endDate) {
        alert('날짜 범위를 먼저 선택해주세요!');
        if (loadingDiv) loadingDiv.style.display = 'none';
        return;
    }
    
    // 요일 라벨
    const dayLabels = {
        'ko': ['일요일', '월요일', '화요일', '수요일', '목요일', '금요일', '토요일'],
        'en': ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'],
        'zh': ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
    };
    
    const currentLang = localStorage.getItem('language') || 'ko';
    const dayName = dayLabels[currentLang][dayOfWeek];
    const timeStr = `${hour}:00 - ${hour + 1}:00`;
    
    // 상세 영역 표시
    const detailBox = document.getElementById('hourly-detail');
    const detailTitle = document.getElementById('hourly-detail-title');
    const detailActiveUsers = document.getElementById('hourly-detail-active-users');
    const detailPageviews = document.getElementById('hourly-detail-total-pageviews');
    
    if (!detailBox) {
        console.error('Hourly detail elements not found');
        if (loadingDiv) loadingDiv.style.display = 'none';
        return;
    }
    
    // 제목 설정
    detailTitle.textContent = `🕐 ${dayName} ${timeStr} 상세 분석`;
    
    // 상세 영역 표시
    detailBox.style.display = 'block';
    
    // 서브 탭 초기화
    const allSubTabs = detailBox.querySelectorAll('.sub-tab');
    const allSubContents = detailBox.querySelectorAll('.sub-tab-content');
    allSubTabs.forEach(tab => tab.classList.remove('active'));
    allSubContents.forEach(content => content.classList.remove('active'));
    
    if (allSubTabs[0]) allSubTabs[0].classList.add('active');
    const fullContent = document.getElementById('hourly-detail-full-content');
    if (fullContent) fullContent.classList.add('active');
    
    // 카테고리 필터 드롭다운 표시
    const fullCategoryFilter = document.getElementById('hourly-detail-full-category-filter');
    if (fullCategoryFilter) {
        fullCategoryFilter.classList.add('visible');
    }
    
    // 스크롤 이동
    setTimeout(() => {
        detailBox.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 100);
    
    try {
        // 기간 내 모든 날짜에 대해 같은 요일의 데이터를 가져오기
        const start = new Date(startDate);
        const end = new Date(endDate);
        const datesToFetch = [];
        
        // 시작일부터 종료일까지 루프하면서 같은 요일인 날짜들 찾기
        const currentDate = new Date(start);
        while (currentDate <= end) {
            if (currentDate.getDay() === dayOfWeek) {
                datesToFetch.push(formatDateForAPI(currentDate));
            }
            currentDate.setDate(currentDate.getDate() + 1);
        }
        
        // 가장 최근 날짜의 데이터만 가져오기 (또는 모든 날짜의 평균을 구할 수도 있음)
        if (datesToFetch.length === 0) {
            throw new Error('선택한 기간에 해당 요일이 없습니다.');
        }
        
        const targetDate = datesToFetch[datesToFetch.length - 1]; // 가장 최근 날짜
        
        // API 호출
        const response = await fetch(`/api/hourly-detail-pageviews?date=${targetDate}&hour=${hour}`);
        const data = await response.json();
        
        // 데이터를 전역 변수에 저장 (필터링용)
        hourlyDetailData = data;
        
        // 요약 정보 표시
        detailActiveUsers.textContent = new Intl.NumberFormat('ko-KR').format(data.activeUsers || 0);
        const totalPageViews = data.pageViews ? 
            data.pageViews.reduce((sum, item) => sum + item.pageViews, 0) : 0;
        detailPageviews.textContent = new Intl.NumberFormat('ko-KR').format(Math.round(totalPageViews));
        
        // 전체 제목 테이블 업데이트
        if (data.pageViews && data.pageViews.length > 0) {
            updatePageViewsTable(data.pageViews, 'hourly-detail-page-title-views');
        } else {
            const noDataText = window.t ? window.t('messages.noData') : '데이터가 없습니다';
            document.getElementById('hourly-detail-page-title-views').innerHTML = 
                `<tr><td colspan="4" style="text-align: center; color: #999;">${noDataText}</td></tr>`;
        }
        
        // 접두어 데이터 업데이트
        if (data.pageViews) {
            updatePrefixViewsTable(data.pageViews, 'hourly-detail-prefix1-views', 1);
            updatePrefixViewsTable(data.pageViews, 'hourly-detail-prefix2-views', 2);
            updatePrefixViewsTable(data.pageViews, 'hourly-detail-prefix3-views', 3);
        }
        
        // 카테고리별 데이터 업데이트
        if (data.pageViews && window.wpCategoryData) {
            updateCategoryTableForHourly(data.pageViews, 'hourly-detail-category-views');
        }
        
        // 카테고리 드롭다운 초기화
        if (window.wpCategoryData && Object.keys(window.wpCategoryData.categories || {}).length > 0) {
            if (window.initializeCategoryDropdown) {
                window.initializeCategoryDropdown('hourly-detail', data.pageViews);
            }
            if (window.checkCategoryDataAvailability) {
                window.checkCategoryDataAvailability('hourly-detail');
            }
        }
        
    } catch (error) {
        console.error('시간대별 상세 데이터 로드 실패:', error);
        alert('데이터를 불러오는데 실패했습니다.');
    } finally {
        // 로딩 숨기기
        if (loadingDiv) loadingDiv.style.display = 'none';
    }
}

// 날짜 형식 변환 (Date 객체를 YYYY-MM-DD로)
function formatDateForAPI(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

// 시간대별 상세 닫기
function closeHourlyDetail() {
    const detailBox = document.getElementById('hourly-detail');
    if (detailBox) {
        detailBox.style.display = 'none';
    }
}

// 카테고리별 테이블 업데이트 (시간대별용)
function updateCategoryTableForHourly(pageViews, tableId) {
    // 기존 updateCategoryTableForDaily 함수와 동일한 로직 사용
    if (!window.wpCategoryData || !pageViews) return;
    
    const tableBody = document.getElementById(tableId);
    if (!tableBody) return;
    
    // 카테고리별 조회수 집계
    const categoryViews = {};
    
    pageViews.forEach(page => {
        const postId = window.extractPostId ? window.extractPostId(page.pagePath) : null;
        if (postId && window.wpCategoryData.posts[postId]) {
            window.wpCategoryData.posts[postId].forEach(catId => {
                if (!categoryViews[catId]) {
                    categoryViews[catId] = 0;
                }
                categoryViews[catId] += page.pageViews;
            });
        }
    });
    
    // 배열로 변환하고 정렬
    const sortedCategories = Object.entries(categoryViews)
        .map(([catId, views]) => ({
            id: catId,
            name: window.wpCategoryData.categories[catId] || '알 수 없음',
            views: views
        }))
        .sort((a, b) => b.views - a.views);
    
    // 총 조회수 계산
    const totalViews = sortedCategories.reduce((sum, cat) => sum + cat.views, 0);
    
    // 테이블 업데이트
    tableBody.innerHTML = '';
    
    sortedCategories.forEach((cat, index) => {
        const row = document.createElement('tr');
        
        // 순번
        const numCell = document.createElement('td');
        numCell.textContent = index + 1;
        row.appendChild(numCell);
        
        // 카테고리명
        const nameCell = document.createElement('td');
        nameCell.textContent = cat.name.replace(/&amp;/g, '&');
        row.appendChild(nameCell);
        
        // 조회수
        const viewsCell = document.createElement('td');
        viewsCell.textContent = Math.round(cat.views);
        row.appendChild(viewsCell);
        
        // 비율 (프로그레스 바)
        const ratioCell = document.createElement('td');
        const percentage = totalViews > 0 ? (cat.views / totalViews * 100) : 0;
        
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
        percentText.style.marginLeft = '10px';
        percentText.style.fontSize = '0.9em';
        
        progressBar.appendChild(progressValue);
        progressContainer.appendChild(progressBar);
        progressContainer.appendChild(percentText);
        ratioCell.appendChild(progressContainer);
        
        row.appendChild(ratioCell);
        tableBody.appendChild(row);
    });
    
    if (sortedCategories.length === 0) {
        const noDataText = window.t ? window.t('messages.noData') : '데이터가 없습니다';
        tableBody.innerHTML = `<tr><td colspan="4" style="text-align: center; color: #999;">${noDataText}</td></tr>`;
    }
}

// 시간대별 상세의 카테고리 필터링
function filterHourlyDetailByCategory(categoryId) {
    if (!hourlyDetailData || !hourlyDetailData.pageViews) return;
    
    let filteredData = hourlyDetailData.pageViews;
    
    if (categoryId && window.wpCategoryData) {
        filteredData = hourlyDetailData.pageViews.filter(page => {
            const postId = window.extractPostId ? window.extractPostId(page.pagePath) : null;
            return postId && window.wpCategoryData.posts[postId] && 
                   window.wpCategoryData.posts[postId].includes(parseInt(categoryId));
        });
    }
    
    updatePageViewsTable(filteredData, 'hourly-detail-page-title-views');
}

// window 객체에 함수 등록 (다국어 전환시 사용)
window.renderHeatmap = renderHeatmap;
window.heatmapData = heatmapData;
window.showHourlyDetail = showHourlyDetail;
window.closeHourlyDetail = closeHourlyDetail;
window.updateCategoryTableForHourly = updateCategoryTableForHourly;
window.filterHourlyDetailByCategory = filterHourlyDetailByCategory;