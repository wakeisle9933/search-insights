// 시간대별 사용자 활동 히트맵 관련 함수들 🔥

let heatmapChart = null;
let heatmapData = null;

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
                     style="background-color: ${bgColor};"
                     data-day="${dataIndex}" 
                     data-hour="${hour}" 
                     data-value="${value}">
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

// window 객체에 함수 등록 (다국어 전환시 사용)
window.renderHeatmap = renderHeatmap;
window.heatmapData = heatmapData;