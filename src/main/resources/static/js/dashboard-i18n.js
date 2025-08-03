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
      comparison: "기간 비교",
      dailyChart: "차트 및 인사이트",
      backlinkCheck: "백링크",
      flow: "플로우",
      reportSend: "리포트 발송"
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
      comparisonAnalysis: "기간 비교 분석",
      dailyChart: "차트 및 인사이트",
      backlinkCheck: "백링크",
      flow: "사용자 플로우 분석",
      reportSend: "Search Insights 리포트 발송",
      demographicsHeatmap: "성별/연령별 분석"
    },
    
    // 섹션 제목
    sectionTitles: {
      dailyChartTitle: "일간 차트",
      pageviewsByTitle: "페이지 제목별 조회수",
      pageviewsByTitleToday: "페이지 제목별 조회수 (오늘)",
      pageviewsByTitleCustom: "페이지 제목별 조회수 (지정 기간)",
      pageviewsByPrefix1: "접두어별 조회수 (1단어)",
      pageviewsByPrefix2: "접두어별 조회수 (2단어)",
      pageviewsByPrefix3: "접두어별 조회수 (3단어)",
      pageviewsByCategory: "카테고리별 조회수",
      categoryComparison: "카테고리별 비교",
      postDetailComparison: "포스트별 상세 비교",
      backlinkSummary: "백링크 상위 10개 누적합",
      backlinkAnalysis: "백링크 분석 (외부 유입)",
      reportContents: "리포트 포함 내용",
      recipientInfo: "수신자 정보",
      trafficSourceByDomain: "도메인별 트래픽 소스",
      trafficByDomainTop10: "도메인별 접속량 TOP 7",
      topPages: "상위 페이지 TOP 20",
      flowDetail: "사용자 플로우 상세"
    },
    
    // 버튼
    buttons: {
      fetch: "조회하기",
      compare: "비교하기",
      syncCategories: "카테고리 추가 동기화",
      resetCategories: "카테고리 전체 초기화",
      themeToggle: "테마 변경",
      languageToggle: "언어 변경",
      sendReport: "리포트 발송하기"
    },
    
    // 테이블 헤더
    tableHeaders: {
      rank: "순번",
      pageTitle: "페이지 제목",
      pagePath: "페이지 경로",
      postTitle: "포스트 제목",
      views: "조회수",
      pageViews: "조회수",
      ratio: "비율",
      actions: "분석",
      prefix: "접두어",
      category: "카테고리",
      periodA: "기간 A",
      periodB: "기간 B",
      change: "변화량",
      changeRate: "변화율",
      periodARatio: "기간 A 비중",
      periodBRatio: "기간 B 비중",
      sourceSite: "출처 사이트",
      landingPage: "랜딩 페이지",
      sessions: "세션수",
      domain: "도메인",
      channel: "채널"
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
      periodB: "기간 B (최근)",
      selectPeriod: "기간 선택:",
      autoRefresh: "자동 새로고침 (10초)",
      reportPeriod: "리포트 기간 선택",
      recipient: "수신자:",
      domainFilter: "도메인 선택:",
      searchPage: "페이지 검색:"
    },
    
    // 차트 라벨
    chartLabels: {
      activeUsers: "활성 사용자",
      totalPageviews: "전체 조회수"
    },
    
    // 빠른 선택 버튼
    quickDates: {
      today: "오늘",
      days3: "3일",
      days7: "7일",
      weeks2: "2주",
      month1: "한달",
      months3: "3개월"
    },
    
    // 필터 버튼
    filters: {
      all: "전체 보기",
      top25up: "TOP 25 상승",
      top25down: "TOP 25 하락",
      surge: "급상승 (100%↑)",
      allDomains: "전체 도메인"
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
      categoryDetailTitle: "{category} 카테고리 포스트별 상세 비교",
      dailyChartDesc: "일별 방문자 추이, 시간대별 활동 패턴, 성별/연령별 분석까지! 다양한 인사이트를 한눈에 확인하세요. 차트를 클릭하면 상세 데이터를 볼 수 있어요.",
      backlinkDesc: "어느 사이트에서 우리 사이트로 유입되었는지 확인해보세요! 외부 참조 트래픽을 분석합니다.",
      flowDesc: "사용자들이 어떤 경로로 사이트를 탐색하는지 분석해보세요! 페이지별 이동 경로를 시각화합니다.",
      loadingChart: "차트 데이터를 불러오는 중...",
      loadingData: "데이터를 불러오는 중...",
      selectDateRange: "날짜를 선택하고 조회하기 버튼을 클릭해주세요!",
      selectCompareDates: "기간을 선택하고 비교하기 버튼을 클릭해주세요!",
      comparingData: "데이터를 비교하는 중...",
      reportSendDesc: "원하는 기간의 Search Insights 리포트를 이메일로 발송해보세요! Excel 파일로 상세한 분석 데이터를 받아보실 수 있어요.",
      recipientNote: "application.properties에 설정된 이메일 주소로 발송됩니다.",
      reportSending: "리포트를 생성하고 발송 중입니다...",
      reportSent: "리포트가 성공적으로 발송되었습니다!",
      reportFailed: "리포트 발송에 실패했습니다.",
      searchConsoleDelay: "구글 Search Console 데이터는 최대 3일의 지연이 있습니다. (오늘 날짜 기준 3일 전까지의 데이터만 사용 가능)",
      selectDateForHeatmap: "날짜를 선택하면 시간대별 활동 히트맵이 표시됩니다!",
      loadingHeatmap: "히트맵 데이터를 불러오는 중...",
      demographicsDesc: "사용자의 성별과 연령대를 분석해서 타겟 마케팅에 활용하세요!",
      selectDateForDemographics: "날짜를 선택하면 성별/연령별 분석이 표시됩니다!",
      loadingDemographics: "성별/연령별 데이터를 불러오는 중...",
      selectDateForTrafficSource: "날짜를 선택하면 도메인별 트래픽 분포가 표시됩니다!",
      selectDateForTrafficTable: "날짜를 선택하면 도메인별 접속량이 표시됩니다!",
      loadingTrafficSource: "트래픽 소스 데이터를 불러오는 중...",
      loadingTrafficTable: "도메인별 접속량을 불러오는 중...",
      loadingDemographicsDetail: "성별/연령별 상세 데이터를 불러오는 중..."
    },
    
    // 히트맵
    heatmap: {
      title: "시간대별 활동",
      less: "적음",
      more: "많음",
      users: "사용자"
    },
    
    // 성별/연령별 분석
    demographics: {
      title: "성별/연령별 활동",
      activeUsers: "활성 사용자",
      pageViews: "페이지뷰",
      male: "남성",
      female: "여성"
    },
    
    // 기타
    misc: {
      dataLoading: "여기에 데이터가 동적으로 삽입됩니다"
    },
    
    // 플레이스홀더
    placeholders: {
      searchPage: "페이지 제목 또는 URL 입력"
    },
    
    // 플로우 상세
    flowDetail: {
      totalViews: "총 조회수",
      previousPage: "어디서 왔나? (이전 페이지)",
      nextPage: "어디로 갔나? (다음 페이지)",
      siteExit: "사이트 이탈",
      analyzeFlow: "플로우 분석",
      analyzingFlow: "플로우 데이터를 분석하는 중...",
      searchHelp: "목록에 없는 페이지도 검색하면 분석할 수 있습니다"
    },
    
    // 리포트 내용
    reportContents: {
      searchAnalytics: "검색 성과 분석 (쿼리별, 페이지별, 기기별)",
      prefixAnalysis: "접두어 분석 (1~3단어)",
      pageViewAnalysis: "페이지뷰 분석 (제목별, 경로별)",
      backlinks: "백링크 분석",
      dailyTrends: "일간 추이 분석"
    },
    
    // 에러 메시지
    errors: {
      dateRangeInvalid: "시작일이 종료일보다 늦을 수 없어요!",
      periodDateInvalid: "기간 {period}의 시작일이 종료일보다 늦을 수 없어요!",
      selectAllDates: "시작일과 종료일을 모두 선택해주세요!",
      selectBothPeriods: "두 기간의 시작일과 종료일을 모두 선택해주세요!",
      noMatchingData: "조건에 맞는 데이터가 없어요!",
      categoryDataNotFound: "카테고리 데이터를 찾을 수 없어요!",
      categoryDataEmpty: "카테고리 데이터가 비어있어요!",
      loadDetailFailed: "상세 데이터를 불러오는데 실패했습니다.",
      searchConsoleDelay: "구글 Search Console 데이터는 3일의 지연이 있습니다.\n종료일은 {maxDate} 이전이어야 합니다."
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
      categoryPostDetail: "{category} 카테고리 포스트별 상세 비교",
      dailyChartDetail: "상세 분석"
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
      comparison: "Period Compare",
      dailyChart: "Charts & Insights",
      backlinkCheck: "Backlinks",
      flow: "Flow",
      reportSend: "Send Report"
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
      comparisonAnalysis: "Period Comparison Analysis",
      dailyChart: "Charts & Insights",
      backlinkCheck: "Backlinks",
      flow: "User Flow Analysis",
      reportSend: "Search Insights Report",
      demographicsHeatmap: "Gender/Age Analysis"
    },
    
    // Section titles
    sectionTitles: {
      dailyChartTitle: "Daily Chart",
      pageviewsByTitle: "Pageviews by Title",
      pageviewsByTitleToday: "Pageviews by Title (Today)",
      pageviewsByTitleCustom: "Pageviews by Title (Custom Period)",
      pageviewsByPrefix1: "Pageviews by Prefix (1 word)",
      pageviewsByPrefix2: "Pageviews by Prefix (2 words)",
      pageviewsByPrefix3: "Pageviews by Prefix (3 words)",
      pageviewsByCategory: "Pageviews by Category",
      categoryComparison: "Category Comparison",
      postDetailComparison: "Post Detail Comparison",
      backlinkSummary: "Top 10 Backlinks Summary",
      backlinkAnalysis: "Backlink Analysis (Referral Traffic)",
      reportContents: "Report Contents",
      recipientInfo: "Recipient Information",
      trafficSourceByDomain: "Traffic Source by Domain",
      trafficByDomainTop10: "Top 7 Traffic by Domain",
      topPages: "Top 20 Pages",
      flowDetail: "User Flow Detail"
    },
    
    // Buttons
    buttons: {
      fetch: "Fetch",
      compare: "Compare",
      syncCategories: "Sync Categories",
      resetCategories: "Reset All Categories",
      themeToggle: "Toggle Theme",
      languageToggle: "Change Language",
      sendReport: "Send Report"
    },
    
    // Table headers
    tableHeaders: {
      rank: "Rank",
      pageTitle: "Page Title",
      pagePath: "Page Path",
      postTitle: "Post Title",
      views: "Views",
      pageViews: "Page Views",
      ratio: "Ratio",
      actions: "Analysis",
      prefix: "Prefix",
      category: "Category",
      periodA: "Period A",
      periodB: "Period B",
      change: "Change",
      changeRate: "Change %",
      periodARatio: "Period A %",
      periodBRatio: "Period B %",
      sourceSite: "Source Site",
      landingPage: "Landing Page",
      sessions: "Sessions",
      domain: "Domain",
      channel: "Channel"
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
      periodB: "Period B (Recent)",
      selectPeriod: "Select Period:",
      autoRefresh: "Auto Refresh (10s)",
      reportPeriod: "Select Report Period",
      recipient: "Recipient:",
      domainFilter: "Select Domain:",
      searchPage: "Search Page:"
    },
    
    // Chart labels
    chartLabels: {
      activeUsers: "Active Users",
      totalPageviews: "Total Pageviews"
    },
    
    // Quick date buttons
    quickDates: {
      today: "Today",
      days3: "3 Days",
      days7: "7 Days",
      weeks2: "2 Weeks",
      month1: "1 Month",
      months3: "3 Months"
    },
    
    // Filter buttons
    filters: {
      all: "View All",
      top25up: "TOP 25 Up",
      top25down: "TOP 25 Down",
      surge: "Surge (100%↑)",
      allDomains: "All Domains"
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
      categoryDetailTitle: "{category} Category Post Detail Comparison",
      dailyChartDesc: "Check daily visitor trends, hourly activity patterns, and demographics analysis! Get various insights at a glance. Click on charts to view detailed data.",
      backlinkDesc: "Check which sites are referring traffic to your site! Analyze external referral traffic.",
      flowDesc: "Analyze how users navigate through your site! Visualize page-to-page navigation paths.",
      loadingChart: "Loading chart data...",
      loadingData: "Loading data...",
      selectDateRange: "Please select dates and click the search button!",
      selectCompareDates: "Please select periods and click the compare button!",
      comparingData: "Comparing data...",
      reportSendDesc: "Send Search Insights reports for your desired period via email! Get detailed analytics data in Excel format.",
      recipientNote: "Will be sent to the email address configured in application.properties.",
      reportSending: "Generating and sending report...",
      reportSent: "Report sent successfully!",
      reportFailed: "Failed to send report.",
      searchConsoleDelay: "Google Search Console data has up to 3 days delay. (Only data up to 3 days before today is available)",
      selectDateForHeatmap: "Select dates to view hourly activity heatmap!",
      loadingHeatmap: "Loading heatmap data...",
      demographicsDesc: "Analyze user demographics by gender and age for targeted marketing!",
      selectDateForDemographics: "Select dates to view gender/age demographics analysis!",
      loadingDemographics: "Loading gender/age demographics data...",
      selectDateForTrafficSource: "Select dates to view traffic distribution by domain!",
      selectDateForTrafficTable: "Select dates to view traffic volume by domain!",
      loadingTrafficSource: "Loading traffic source data...",
      loadingTrafficTable: "Loading traffic volume by domain...",
      loadingDemographicsDetail: "Loading gender/age demographic detail data..."
    },
    
    // Heatmap
    heatmap: {
      title: "Hourly Activity",
      less: "Less",
      more: "More",
      users: "Users"
    },
    
    // Demographics
    demographics: {
      title: "Gender/Age Activity",
      activeUsers: "Active Users",
      pageViews: "Page Views",
      male: "Male",
      female: "Female"
    },
    
    // Misc
    misc: {
      dataLoading: "Data will be loaded here dynamically"
    },
    
    // Placeholders
    placeholders: {
      searchPage: "Enter page title or URL"
    },
    
    // Flow Detail
    flowDetail: {
      totalViews: "Total Views",
      previousPage: "Where did they come from? (Previous Page)",
      nextPage: "Where did they go? (Next Page)",
      siteExit: "Exit Site",
      analyzeFlow: "Analyze Flow",
      analyzingFlow: "Analyzing flow data...",
      searchHelp: "Pages not in the list can also be analyzed by searching"
    },
    
    // Report contents
    reportContents: {
      searchAnalytics: "Search Performance Analysis (by Query, Page, Device)",
      prefixAnalysis: "Prefix Analysis (1-3 words)",
      pageViewAnalysis: "Pageview Analysis (by Title, Path)",
      backlinks: "Backlink Analysis",
      dailyTrends: "Daily Trend Analysis"
    },
    
    // Error messages
    errors: {
      dateRangeInvalid: "Start date cannot be later than end date!",
      periodDateInvalid: "Period {period} start date cannot be later than end date!",
      selectAllDates: "Please select both start and end dates!",
      selectBothPeriods: "Please select dates for both periods!",
      noMatchingData: "No matching data found!",
      categoryDataNotFound: "Category data not found!",
      categoryDataEmpty: "Category data is empty!",
      loadDetailFailed: "Failed to load detailed data.",
      searchConsoleDelay: "Google Search Console data has a 3-day delay.\nEnd date must be before {maxDate}."
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
      categoryPostDetail: "{category} Category Post Detail Comparison",
      dailyChartDetail: "Detailed Analysis"
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
      comparison: "期间比较",
      dailyChart: "图表及洞察",
      backlinkCheck: "反向链接",
      flow: "流程",
      reportSend: "发送报告"
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
      comparisonAnalysis: "期间比较分析",
      dailyChart: "图表及洞察",
      backlinkCheck: "反向链接",
      flow: "用户流程分析",
      reportSend: "Search Insights 报告发送",
      demographicsHeatmap: "性别/年龄分析"
    },
    
    // 部分标题
    sectionTitles: {
      dailyChartTitle: "日间图表",
      pageviewsByTitle: "按标题统计浏览量",
      pageviewsByTitleToday: "按标题统计浏览量 (今日)",
      pageviewsByTitleCustom: "按标题统计浏览量 (指定期间)",
      pageviewsByPrefix1: "按前缀统计浏览量 (1个词)",
      pageviewsByPrefix2: "按前缀统计浏览量 (2个词)",
      pageviewsByPrefix3: "按前缀统计浏览量 (3个词)",
      pageviewsByCategory: "按分类统计浏览量",
      categoryComparison: "分类比较",
      postDetailComparison: "文章详细比较",
      backlinkSummary: "前10个反向链接汇总",
      backlinkAnalysis: "反向链接分析 (外部流量)",
      reportContents: "报告内容",
      recipientInfo: "收件人信息",
      trafficSourceByDomain: "按域名分类的流量来源",
      trafficByDomainTop10: "按域名访问量前7名",
      topPages: "前20个页面",
      flowDetail: "用户流程详情"
    },
    
    // 按钮
    buttons: {
      fetch: "查询",
      compare: "比较",
      syncCategories: "同步分类",
      resetCategories: "重置所有分类",
      themeToggle: "切换主题",
      languageToggle: "切换语言",
      sendReport: "发送报告"
    },
    
    // 表格标题
    tableHeaders: {
      rank: "排名",
      pageTitle: "页面标题",
      pagePath: "页面路径",
      postTitle: "文章标题",
      views: "浏览量",
      pageViews: "页面浏览量",
      ratio: "比例",
      actions: "分析",
      prefix: "前缀",
      category: "分类",
      periodA: "期间 A",
      periodB: "期间 B",
      change: "变化量",
      changeRate: "变化率",
      periodARatio: "期间 A 占比",
      periodBRatio: "期间 B 占比",
      sourceSite: "来源网站",
      landingPage: "着陆页",
      sessions: "会话数",
      domain: "域名",
      channel: "渠道"
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
      periodB: "期间 B (最近)",
      selectPeriod: "选择期间:",
      autoRefresh: "自动刷新 (10秒)",
      reportPeriod: "选择报告期间",
      recipient: "收件人:",
      domainFilter: "选择域名:",
      searchPage: "搜索页面:"
    },
    
    // 图表标签
    chartLabels: {
      activeUsers: "活跃用户",
      totalPageviews: "总浏览量"
    },
    
    // 快速日期按钮
    quickDates: {
      today: "今天",
      days3: "3天",
      days7: "7天",
      weeks2: "2周",
      month1: "1个月",
      months3: "3个月"
    },
    
    // 筛选按钮
    filters: {
      all: "查看全部",
      top25up: "TOP 25 上升",
      top25down: "TOP 25 下降",
      surge: "急剧上升 (100%↑)",
      allDomains: "全部域名"
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
      categoryDetailTitle: "{category} 分类文章详细比较",
      dailyChartDesc: "查看每日访客趋势、按小时活动模式以及性别/年龄分析！一目了然地获得各种洞察。点击图表可查看详细数据。",
      backlinkDesc: "查看哪些网站向您的网站引流! 分析外部引荐流量。",
      flowDesc: "分析用户如何浏览您的网站！可视化页面间的导航路径。",
      loadingChart: "正在加载图表数据...",
      loadingData: "正在加载数据...",
      selectDateRange: "请选择日期并点击查询按钮!",
      selectCompareDates: "请选择期间并点击比较按钮!",
      comparingData: "正在比较数据...",
      reportSendDesc: "通过电子邮件发送您所需期间的Search Insights报告！以Excel格式获取详细的分析数据。",
      recipientNote: "将发送到application.properties中配置的电子邮件地址。",
      reportSending: "正在生成并发送报告...",
      reportSent: "报告发送成功！",
      reportFailed: "报告发送失败。",
      searchConsoleDelay: "谷歌Search Console数据最多有 3 天的延迟。（仅可使用今天之前 3 天的数据）",
      selectDateForHeatmap: "选择日期即可查看按小时活动热图!",
      loadingHeatmap: "正在加载热图数据...",
      demographicsDesc: "按性别和年龄分析用户人口统计数据，用于精准营销！",
      selectDateForDemographics: "选择日期以查看性别/年龄人口统计分析！",
      loadingDemographics: "正在加载性别/年龄人口统计数据...",
      selectDateForTrafficSource: "选择日期以查看按域名分类的流量分布！",
      selectDateForTrafficTable: "选择日期以查看按域名的访问量！",
      loadingTrafficSource: "正在加载流量来源数据...",
      loadingTrafficTable: "正在加载按域名的访问量...",
      loadingDemographicsDetail: "正在加载性别/年龄详细数据..."
    },
    
    // 热图
    heatmap: {
      title: "按小时活动",
      less: "少",
      more: "多",
      users: "用户"
    },
    
    // 人口统计
    demographics: {
      title: "性别/年龄活动",
      activeUsers: "活跃用户",
      pageViews: "页面浏览量",
      male: "男性",
      female: "女性"
    },
    
    // 其他
    misc: {
      dataLoading: "数据将在此处动态加载"
    },
    
    // 占位符
    placeholders: {
      searchPage: "输入页面标题或URL"
    },
    
    // 流程详情
    flowDetail: {
      totalViews: "总浏览量",
      previousPage: "从哪里来？（上一页）",
      nextPage: "到哪里去？（下一页）",
      siteExit: "离开网站",
      analyzeFlow: "分析流程",
      analyzingFlow: "正在分析流程数据...",
      searchHelp: "列表中没有的页面也可以通过搜索进行分析"
    },
    
    // 报告内容
    reportContents: {
      searchAnalytics: "搜索效果分析（按查询、页面、设备）",
      prefixAnalysis: "前缀分析（1-3个词）",
      pageViewAnalysis: "页面浏览量分析（按标题、路径）",
      backlinks: "反向链接分析",
      dailyTrends: "每日趋势分析"
    },
    
    // 错误消息
    errors: {
      dateRangeInvalid: "开始日期不能晚于结束日期！",
      periodDateInvalid: "期间 {period} 的开始日期不能晚于结束日期！",
      selectAllDates: "请选择开始和结束日期！",
      selectBothPeriods: "请为两个期间选择日期！",
      noMatchingData: "没有符合条件的数据！",
      categoryDataNotFound: "找不到分类数据！",
      categoryDataEmpty: "分类数据为空！",
      loadDetailFailed: "加载详细数据失败。",
      searchConsoleDelay: "谷歌Search Console数据有 3 天延迟。\n结束日期必须在 {maxDate} 之前。"
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
      categoryPostDetail: "{category} 分类文章详细比较",
      dailyChartDetail: "详细分析"
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
    
    // 일간 차트가 그려져 있으면 다시 그리기
    if (typeof window.renderDailyChart === 'function' && window.dailyChart && window.dailyChartData && window.dailyChartData.dates && window.dailyChartData.dates.length > 0) {
      if (document.getElementById('daily-chart-content') && document.getElementById('daily-chart-content').classList.contains('active')) {
        try {
          window.renderDailyChart();
        } catch (error) {
          // 에러 발생 시 조용히 무시
        }
      }
    }
    
    // 히트맵이 그려져 있으면 다시 그리기 🔥
    if (typeof window.renderHeatmap === 'function' && window.heatmapData) {
      if (document.getElementById('heatmap-container')) {
        try {
          window.renderHeatmap(window.heatmapData);
        } catch (error) {
          // 에러 발생 시 조용히 무시
        }
      }
    }
    
    // 성별/연령별 차트가 그려져 있으면 다시 그리기 💕
    if (typeof window.renderDemographicsHeatmap === 'function' && window.demographicsHeatmapData) {
      if (document.getElementById('demographics-heatmap-container')) {
        try {
          window.renderDemographicsHeatmap(window.demographicsHeatmapData);
        } catch (error) {
          // 에러 발생 시 조용히 무시
        }
      }
    }
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
  
  // 백링크 업데이트 시간이 있다면 갱신
  const backlinkTimeElement = document.getElementById('backlink-update-time');
  if (backlinkTimeElement && !backlinkTimeElement.textContent.endsWith(': -')) {
    if (typeof updateBacklinkTime === 'function') {
      updateBacklinkTime();
    }
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
  const allDomainsText = t('filters.allDomains');
  
  // 모든 카테고리 드롭다운 찾기
  const categoryDropdowns = [
    document.getElementById('today-full-category-select'),
    document.getElementById('custom-date-full-category-select'),
    document.getElementById('daily-detail-full-category-select'),
    document.getElementById('hourly-detail-full-category-select'),
    document.getElementById('demographics-detail-full-category-select')
  ];
  
  categoryDropdowns.forEach(dropdown => {
    if (dropdown && dropdown.options.length > 0) {
      // 첫 번째 옵션(전체 카테고리)의 텍스트 업데이트
      dropdown.options[0].textContent = allCategoriesText;
    }
  });
  
  // 백링크 도메인 드롭다운 업데이트
  const domainDropdown = document.getElementById('backlink-domain-filter');
  if (domainDropdown && domainDropdown.options.length > 0) {
    domainDropdown.options[0].textContent = allDomainsText;
  }
}

// 다음 언어로 순환
function cycleLanguage() {
  const languages = Object.keys(translations);
  const currentIndex = languages.indexOf(currentLanguage);
  const nextIndex = (currentIndex + 1) % languages.length;
  changeLanguage(languages[nextIndex]);
}

// 날짜 입력 필드 언어 설정 함수
function updateDateInputLanguage() {
  const dateInputs = document.querySelectorAll('input[type="date"]');
  const currentLang = getCurrentLanguage();
  
  dateInputs.forEach(input => {
    // 언어에 따른 lang 속성 설정
    if (currentLang === 'en') {
      input.setAttribute('lang', 'en-US');
    } else if (currentLang === 'zh') {
      input.setAttribute('lang', 'zh-CN');
    } else {
      input.setAttribute('lang', 'ko-KR');
    }
  });
}

// window 객체에 함수 등록
window.t = t;
window.tTemplate = tTemplate;
window.changeLanguage = changeLanguage;
window.updateAllTranslations = updateAllTranslations;
window.updateDateInputLanguage = updateDateInputLanguage;
window.getCurrentLanguage = getCurrentLanguage;
window.cycleLanguage = cycleLanguage;

// 페이지 로드 시 번역 자동 적용
document.addEventListener('DOMContentLoaded', function() {
  // 저장된 언어 설정 복원
  const savedLang = localStorage.getItem('language');
  if (savedLang) {
    currentLanguage = savedLang;
    const langSelect = document.querySelector('.language-select');
    if (langSelect) {
      langSelect.value = savedLang;
    }
  }
  updateAllTranslations();
  
  // 날짜 입력 필드 언어 설정
  if (typeof updateDateInputLanguage === 'function') {
    updateDateInputLanguage();
  }
});