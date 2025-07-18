// 다국어 지원을 위한 번역 데이터
const translations = {
  ko: {
    // 페이지 제목
    pageTitle: "실시간 대시보드",
    
    // 메인 탭
    mainTabs: {
      today: "오늘 전체",
      last30min: "최근 30분",
      customDate: "날짜 지정",
      comparison: "기간 비교"
    },
    
    // 서브 탭
    subTabs: {
      allTitles: "전체 제목",
      prefix1: "접두어 (1단어)",
      prefix2: "접두어 (2단어)",
      prefix3: "접두어 (3단어)",
      byCategory: "카테고리별"
    },
    
    // 대시보드 박스 제목
    dashboardTitles: {
      todayActiveUsers: "오늘 전체 활성 사용자",
      todayPageviews: "오늘 전체 조회수",
      last30minActiveUsers: "최근 30분 활성 사용자",
      last30minPageviews: "최근 30분 전체 조회수",
      customActiveUsers: "지정 기간 활성 사용자",
      customPageviews: "지정 기간 전체 조회수",
      comparisonAnalysis: "기간 비교 분석"
    },
    
    // 섹션 제목
    sectionTitles: {
      pageviewsByTitle: "페이지 제목별 조회수",
      pageviewsByTitleToday: "페이지 제목별 조회수 (오늘)",
      pageviewsByTitleCustom: "페이지 제목별 조회수 (지정 기간)",
      pageviewsByPrefix1: "접두어별 조회수 (1단어)",
      pageviewsByPrefix2: "접두어별 조회수 (2단어)",
      pageviewsByPrefix3: "접두어별 조회수 (3단어)",
      pageviewsByCategory: "카테고리별 조회수",
      categoryComparison: "카테고리별 비교",
      postDetailComparison: "포스트별 상세 비교"
    },
    
    // 버튼
    buttons: {
      fetch: "조회하기",
      compare: "비교하기",
      syncCategories: "카테고리 추가 동기화",
      resetCategories: "카테고리 전체 초기화",
      themeToggle: "테마 변경",
      languageToggle: "언어 변경"
    },
    
    // 테이블 헤더
    tableHeaders: {
      rank: "순번",
      pageTitle: "페이지 제목",
      postTitle: "포스트 제목",
      views: "조회수",
      ratio: "비율",
      prefix: "접두어",
      category: "카테고리",
      periodA: "기간 A",
      periodB: "기간 B",
      change: "변화량",
      changeRate: "변화율",
      periodARatio: "기간 A 비중",
      periodBRatio: "기간 B 비중"
    },
    
    // 라벨
    labels: {
      categorySelect: "카테고리 선택:",
      allCategories: "전체 카테고리",
      autoUpdate: "오늘 포함시 자동 업데이트",
      quickSelect: "빠른 선택:",
      excludeToday: "전일 기준 (오늘 제외)",
      lastUpdate: "마지막 업데이트",
      periodA: "기간 A (이전)",
      periodB: "기간 B (최근)"
    },
    
    // 빠른 선택 버튼
    quickDates: {
      today: "오늘",
      days3: "3일",
      days7: "7일",
      weeks2: "2주",
      month1: "한달"
    },
    
    // 필터 버튼
    filters: {
      all: "전체 보기",
      top25up: "TOP 25 상승",
      top25down: "TOP 25 하락",
      surge: "급상승 (100%↑)"
    },
    
    // 메시지
    messages: {
      noTitle: "(제목 없음)",
      noData: "데이터가 없거나 아직 카테고리가 동기화되지 않았어요!",
      clickDetail: "(클릭하여 상세보기)",
      noMatchData: "조건에 맞는 데이터가 없어요!",
      syncRequired: "아직 동기화된 카테고리 데이터가 없어요! 동기화 버튼을 눌러주세요!",
      syncInProgress: "동기화 중...",
      syncInProgressDetail: "카테고리 동기화 중... 조금만 기다려주세요!",
      syncInProgressFull: "전체 카테고리 동기화 중... 시간이 조금 걸릴 수 있어요!",
      syncComplete: "동기화 완료! 카테고리 데이터가 최신 상태예요!",
      syncFailed: "동기화 실패! 다시 시도해주세요!",
      selectDates: "시작일과 종료일을 모두 선택해주세요!",
      selectBothPeriods: "두 기간의 시작일과 종료일을 모두 선택해주세요!",
      invalidDateRange: "시작일이 종료일보다 늦을 수 없어요!",
      periodInvalidDate: "기간 {period}의 시작일이 종료일보다 늦을 수 없어요!",
      comparisonDesc: "기간 A(이전)에서 기간 B(최근)로의 변화를 분석해요! 상승/하락 트렌드를 한눈에 확인하세요.",
      categoryDetailTitle: "{category} 카테고리 포스트별 상세 비교"
    },
    
    // 기타
    misc: {
      dataLoading: "여기에 데이터가 동적으로 삽입됩니다"
    },
    
    // 에러 메시지
    errors: {
      dateRangeInvalid: "시작일이 종료일보다 늦을 수 없어요!",
      periodDateInvalid: "기간 {period}의 시작일이 종료일보다 늦을 수 없어요!",
      selectAllDates: "시작일과 종료일을 모두 선택해주세요!",
      selectBothPeriods: "두 기간의 시작일과 종료일을 모두 선택해주세요!",
      noMatchingData: "조건에 맞는 데이터가 없어요!",
      categoryDataNotFound: "카테고리 데이터를 찾을 수 없어요!",
      categoryDataEmpty: "카테고리 데이터가 비어있어요!"
    },
    
    // 콘솔 에러 메시지
    console: {
      todayDataFetchFailed: "오늘 데이터 가져오기 실패",
      last30minDataFetchFailed: "최근 30분 데이터 가져오기 실패",
      customDateDataFetchFailed: "날짜 지정 데이터 가져오기 실패",
      comparisonDataFetchFailed: "비교 데이터 가져오기 실패",
      syncStatusCheckFailed: "동기화 상태 확인 실패",
      categoryDataLoadFailed: "카테고리 데이터 로드 실패",
      categorySyncFailed: "카테고리 동기화 실패"
    },
    
    // UI 텍스트
    ui: {
      clickForDetail: "(클릭하여 상세보기)",
      categoryPostDetail: "{category} 카테고리 포스트별 상세 비교"
    }
  },
  
  en: {
    // Page title
    pageTitle: "Real-time Dashboard",
    
    // Main tabs
    mainTabs: {
      today: "Today",
      last30min: "Last 30 min",
      customDate: "Custom Date",
      comparison: "Period Compare"
    },
    
    // Sub tabs
    subTabs: {
      allTitles: "All Titles",
      prefix1: "Prefix (1 word)",
      prefix2: "Prefix (2 words)",
      prefix3: "Prefix (3 words)",
      byCategory: "By Category"
    },
    
    // Dashboard box titles
    dashboardTitles: {
      todayActiveUsers: "Today's Active Users",
      todayPageviews: "Today's Total Pageviews",
      last30minActiveUsers: "Last 30min Active Users",
      last30minPageviews: "Last 30min Total Pageviews",
      customActiveUsers: "Custom Period Active Users",
      customPageviews: "Custom Period Total Pageviews",
      comparisonAnalysis: "Period Comparison Analysis"
    },
    
    // Section titles
    sectionTitles: {
      pageviewsByTitle: "Pageviews by Title",
      pageviewsByTitleToday: "Pageviews by Title (Today)",
      pageviewsByTitleCustom: "Pageviews by Title (Custom Period)",
      pageviewsByPrefix1: "Pageviews by Prefix (1 word)",
      pageviewsByPrefix2: "Pageviews by Prefix (2 words)",
      pageviewsByPrefix3: "Pageviews by Prefix (3 words)",
      pageviewsByCategory: "Pageviews by Category",
      categoryComparison: "Category Comparison",
      postDetailComparison: "Post Detail Comparison"
    },
    
    // Buttons
    buttons: {
      fetch: "Fetch",
      compare: "Compare",
      syncCategories: "Sync Categories",
      resetCategories: "Reset All Categories",
      themeToggle: "Toggle Theme",
      languageToggle: "Change Language"
    },
    
    // Table headers
    tableHeaders: {
      rank: "Rank",
      pageTitle: "Page Title",
      postTitle: "Post Title",
      views: "Views",
      ratio: "Ratio",
      prefix: "Prefix",
      category: "Category",
      periodA: "Period A",
      periodB: "Period B",
      change: "Change",
      changeRate: "Change %",
      periodARatio: "Period A %",
      periodBRatio: "Period B %"
    },
    
    // Labels
    labels: {
      categorySelect: "Select Category:",
      allCategories: "All Categories",
      autoUpdate: "Auto-update when including today",
      quickSelect: "Quick Select:",
      excludeToday: "Exclude Today",
      lastUpdate: "Last Update",
      periodA: "Period A (Previous)",
      periodB: "Period B (Recent)"
    },
    
    // Quick date buttons
    quickDates: {
      today: "Today",
      days3: "3 Days",
      days7: "7 Days",
      weeks2: "2 Weeks",
      month1: "1 Month"
    },
    
    // Filter buttons
    filters: {
      all: "View All",
      top25up: "TOP 25 Up",
      top25down: "TOP 25 Down",
      surge: "Surge (100%↑)"
    },
    
    // Messages
    messages: {
      noTitle: "(No Title)",
      noData: "No data or categories not synced yet!",
      clickDetail: "(Click for details)",
      noMatchData: "No matching data found!",
      syncRequired: "No synced category data yet! Please click sync button!",
      syncInProgress: "Syncing...",
      syncInProgressDetail: "Syncing categories... Please wait!",
      syncInProgressFull: "Full category sync... This may take a while!",
      syncComplete: "Sync complete! Category data is up to date!",
      syncFailed: "Sync failed! Please try again!",
      selectDates: "Please select both start and end dates!",
      selectBothPeriods: "Please select dates for both periods!",
      invalidDateRange: "Start date cannot be later than end date!",
      periodInvalidDate: "Period {period} start date cannot be later than end date!",
      comparisonDesc: "Analyze changes from Period A (previous) to Period B (recent)! Check trends at a glance.",
      categoryDetailTitle: "{category} Category Post Detail Comparison"
    },
    
    // Misc
    misc: {
      dataLoading: "Data will be loaded here dynamically"
    },
    
    // Error messages
    errors: {
      dateRangeInvalid: "Start date cannot be later than end date!",
      periodDateInvalid: "Period {period} start date cannot be later than end date!",
      selectAllDates: "Please select both start and end dates!",
      selectBothPeriods: "Please select dates for both periods!",
      noMatchingData: "No matching data found!",
      categoryDataNotFound: "Category data not found!",
      categoryDataEmpty: "Category data is empty!"
    },
    
    // Console error messages
    console: {
      todayDataFetchFailed: "Failed to fetch today's data",
      last30minDataFetchFailed: "Failed to fetch last 30min data",
      customDateDataFetchFailed: "Failed to fetch custom date data",
      comparisonDataFetchFailed: "Failed to fetch comparison data",
      syncStatusCheckFailed: "Failed to check sync status",
      categoryDataLoadFailed: "Failed to load category data",
      categorySyncFailed: "Failed to sync categories"
    },
    
    // UI text
    ui: {
      clickForDetail: "(Click for details)",
      categoryPostDetail: "{category} Category Post Detail Comparison"
    }
  },
  
  zh: {
    // 页面标题
    pageTitle: "实时仪表板",
    
    // 主标签
    mainTabs: {
      today: "今日全部",
      last30min: "最近30分钟",
      customDate: "日期指定",
      comparison: "期间比较"
    },
    
    // 子标签
    subTabs: {
      allTitles: "全部标题",
      prefix1: "前缀 (1个词)",
      prefix2: "前缀 (2个词)",
      prefix3: "前缀 (3个词)",
      byCategory: "按分类"
    },
    
    // 仪表板框标题
    dashboardTitles: {
      todayActiveUsers: "今日活跃用户",
      todayPageviews: "今日总浏览量",
      last30minActiveUsers: "最近30分钟活跃用户",
      last30minPageviews: "最近30分钟总浏览量",
      customActiveUsers: "指定期间活跃用户",
      customPageviews: "指定期间总浏览量",
      comparisonAnalysis: "期间比较分析"
    },
    
    // 部分标题
    sectionTitles: {
      pageviewsByTitle: "按标题统计浏览量",
      pageviewsByTitleToday: "按标题统计浏览量 (今日)",
      pageviewsByTitleCustom: "按标题统计浏览量 (指定期间)",
      pageviewsByPrefix1: "按前缀统计浏览量 (1个词)",
      pageviewsByPrefix2: "按前缀统计浏览量 (2个词)",
      pageviewsByPrefix3: "按前缀统计浏览量 (3个词)",
      pageviewsByCategory: "按分类统计浏览量",
      categoryComparison: "分类比较",
      postDetailComparison: "文章详细比较"
    },
    
    // 按钮
    buttons: {
      fetch: "查询",
      compare: "比较",
      syncCategories: "同步分类",
      resetCategories: "重置所有分类",
      themeToggle: "切换主题",
      languageToggle: "切换语言"
    },
    
    // 表格标题
    tableHeaders: {
      rank: "排名",
      pageTitle: "页面标题",
      postTitle: "文章标题",
      views: "浏览量",
      ratio: "比例",
      prefix: "前缀",
      category: "分类",
      periodA: "期间 A",
      periodB: "期间 B",
      change: "变化量",
      changeRate: "变化率",
      periodARatio: "期间 A 占比",
      periodBRatio: "期间 B 占比"
    },
    
    // 标签
    labels: {
      categorySelect: "选择分类:",
      allCategories: "所有分类",
      autoUpdate: "包含今天时自动更新",
      quickSelect: "快速选择:",
      excludeToday: "排除今天",
      lastUpdate: "最后更新",
      periodA: "期间 A (之前)",
      periodB: "期间 B (最近)"
    },
    
    // 快速日期按钮
    quickDates: {
      today: "今天",
      days3: "3天",
      days7: "7天",
      weeks2: "2周",
      month1: "1个月"
    },
    
    // 筛选按钮
    filters: {
      all: "查看全部",
      top25up: "TOP 25 上升",
      top25down: "TOP 25 下降",
      surge: "急剧上升 (100%↑)"
    },
    
    // 消息
    messages: {
      noTitle: "(无标题)",
      noData: "没有数据或分类尚未同步!",
      clickDetail: "(点击查看详情)",
      noMatchData: "没有符合条件的数据!",
      syncRequired: "还没有同步的分类数据! 请点击同步按钮!",
      syncInProgress: "同步中...",
      syncInProgressDetail: "正在同步分类... 请稍候!",
      syncInProgressFull: "全部分类同步中... 可能需要一些时间!",
      syncComplete: "同步完成! 分类数据已是最新状态!",
      syncFailed: "同步失败! 请重试!",
      selectDates: "请选择开始和结束日期!",
      selectBothPeriods: "请为两个期间选择日期!",
      invalidDateRange: "开始日期不能晚于结束日期!",
      periodInvalidDate: "期间 {period} 的开始日期不能晚于结束日期!",
      comparisonDesc: "分析从期间 A (之前) 到期间 B (最近) 的变化! 一目了然地查看趋势。",
      categoryDetailTitle: "{category} 分类文章详细比较"
    },
    
    // 其他
    misc: {
      dataLoading: "数据将在此处动态加载"
    },
    
    // 错误消息
    errors: {
      dateRangeInvalid: "开始日期不能晚于结束日期！",
      periodDateInvalid: "期间 {period} 的开始日期不能晚于结束日期！",
      selectAllDates: "请选择开始和结束日期！",
      selectBothPeriods: "请为两个期间选择日期！",
      noMatchingData: "没有符合条件的数据！",
      categoryDataNotFound: "找不到分类数据！",
      categoryDataEmpty: "分类数据为空！"
    },
    
    // 控制台错误消息
    console: {
      todayDataFetchFailed: "获取今日数据失败",
      last30minDataFetchFailed: "获取最近30分钟数据失败",
      customDateDataFetchFailed: "获取指定日期数据失败",
      comparisonDataFetchFailed: "获取比较数据失败",
      syncStatusCheckFailed: "检查同步状态失败",
      categoryDataLoadFailed: "加载分类数据失败",
      categorySyncFailed: "同步分类失败"
    },
    
    // UI 文本
    ui: {
      clickForDetail: "(点击查看详情)",
      categoryPostDetail: "{category} 分类文章详细比较"
    }
  }
};

// 현재 언어 설정
let currentLanguage = localStorage.getItem('language') || 'ko';

// 번역 함수
function t(key) {
  const keys = key.split('.');
  let value = translations[currentLanguage];
  
  for (const k of keys) {
    if (value && typeof value === 'object') {
      value = value[k];
    } else {
      // 키를 찾을 수 없으면 한국어로 폴백
      value = translations.ko;
      for (const fallbackKey of keys) {
        value = value[fallbackKey];
      }
      break;
    }
  }
  
  return value || key; // 번역을 찾을 수 없으면 키 자체를 반환
}

// 템플릿 문자열 처리 함수
function tTemplate(key, params) {
  let text = t(key);
  if (params && typeof params === 'object') {
    Object.keys(params).forEach(param => {
      text = text.replace(`{${param}}`, params[param]);
    });
  }
  return text;
}

// 언어 변경 함수
function changeLanguage(lang) {
  if (translations[lang]) {
    currentLanguage = lang;
    localStorage.setItem('language', lang);
    updateAllTranslations();
  }
}

// 모든 번역 업데이트
function updateAllTranslations() {
  // data-i18n 속성이 있는 모든 요소 업데이트
  document.querySelectorAll('[data-i18n]').forEach(element => {
    const key = element.getAttribute('data-i18n');
    const text = t(key);
    
    // 이모지 보존
    const emojiMatch = element.textContent.match(/^[\u{1F300}-\u{1F9FF}\u{2600}-\u{26FF}\u{2700}-\u{27BF}]+/u);
    if (emojiMatch) {
      element.textContent = emojiMatch[0] + ' ' + text;
    } else {
      element.textContent = text;
    }
  });
  
  // placeholder 업데이트
  document.querySelectorAll('[data-i18n-placeholder]').forEach(element => {
    const key = element.getAttribute('data-i18n-placeholder');
    element.placeholder = t(key);
  });
  
  // title 속성 업데이트
  document.querySelectorAll('[data-i18n-title]').forEach(element => {
    const key = element.getAttribute('data-i18n-title');
    element.title = t(key);
  });
  
  // 페이지 제목 업데이트
  if (document.title.includes('Search Insights')) {
    document.title = t('pageTitle') + ' - Search Insights';
  }
  
  // 언어 셀렉트박스 업데이트
  const languageSelect = document.querySelector('.language-select');
  if (languageSelect) {
    languageSelect.value = currentLanguage;
  }
  
  // 동적으로 생성된 콘텐츠 업데이트 트리거
  if (window.refreshCurrentContent) {
    window.refreshCurrentContent();
  }
  
  // 날짜 입력 필드 언어 업데이트
  if (window.updateDateInputLanguage) {
    window.updateDateInputLanguage();
  }
  
  // 카테고리 드롭다운 옵션 업데이트
  updateCategoryDropdowns();
}

// 현재 언어 가져오기
function getCurrentLanguage() {
  return currentLanguage;
}

// 언어 이름 매핑
const languageNames = {
  ko: '한국어',
  en: 'English',
  zh: '中文'
};

// 카테고리 드롭다운 업데이트 함수
function updateCategoryDropdowns() {
  const allCategoriesText = t('labels.allCategories');
  
  // 모든 카테고리 드롭다운 찾기
  const dropdowns = [
    document.getElementById('today-full-category-select'),
    document.getElementById('custom-date-full-category-select')
  ];
  
  dropdowns.forEach(dropdown => {
    if (dropdown && dropdown.options.length > 0) {
      // 첫 번째 옵션(전체 카테고리)의 텍스트 업데이트
      dropdown.options[0].textContent = allCategoriesText;
    }
  });
}

// 다음 언어로 순환
function cycleLanguage() {
  const languages = Object.keys(translations);
  const currentIndex = languages.indexOf(currentLanguage);
  const nextIndex = (currentIndex + 1) % languages.length;
  changeLanguage(languages[nextIndex]);
}