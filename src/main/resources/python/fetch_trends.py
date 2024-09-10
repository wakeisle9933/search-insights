import sys
import json
import pandas as pd
from pytrends.request import TrendReq

def trending_searches():
    pytrends = TrendReq(hl='ko-KR', tz=540)
    trends = pytrends.trending_searches(pn='south_korea')
    top_trends = trends.head(20).values.flatten().tolist()
    return top_trends

def related_queries(keyword):
    pytrends = TrendReq(hl='ko-KR', tz=540)
    pytrends.build_payload([keyword])
    related_queries = pytrends.related_queries()
    result = related_queries[keyword]
    # 결과를 직렬화 가능한 형태로 변환
    return {
        'top': result['top'].to_dict() if result['top'] is not None else {},
        'rising': result['rising'].to_dict() if result['rising'] is not None else {}
    }

def related_topics(keyword):
    pytrends = TrendReq(hl='ko-KR', tz=540)
    pytrends.build_payload([keyword])
    related_topics = pytrends.related_topics()
    result = related_topics[keyword]
    # 결과를 직렬화 가능한 형태로 변환
    return {
        'top': result['top'].to_dict() if result['top'] is not None else {},
        'rising': result['rising'].to_dict() if result['rising'] is not None else {}
    }

def suggestions(keyword):
    pytrends = TrendReq(hl='ko-KR', tz=540)
    return pytrends.suggestions(keyword)

if __name__ == "__main__":
    method = sys.argv[1]
    result = None

    if method == "trending_searches":
        result = trending_searches()
    elif method in ["related_queries", "related_topics", "suggestions"]:
        keyword = sys.argv[2]
        result = globals()[method](keyword)

    print(json.dumps(result))