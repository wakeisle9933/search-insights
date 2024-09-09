import sys
import json
from pytrends.request import TrendReq

try:
    pytrends = TrendReq(hl='ko-KR', tz=540)
    trends = pytrends.trending_searches(pn='south_korea')

    if trends.empty:
        print(json.dumps([]))  # 빈 리스트 반환
    else:
        top_trends = trends.head(20).values.flatten().tolist()
        print(json.dumps(top_trends))

except Exception as e:
    print(json.dumps([f"Error: {str(e)}"]), file=sys.stderr)
    print(json.dumps([]))  # 표준 출력에는 빈 리스트 출력