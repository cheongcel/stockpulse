package com.stockpulse.stockpulse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 뉴스 수집(NewsService) + 기사별 AI 요약(AiService)을 한 묶음(Digest)으로 캐시한다.
 * 화면(/analyze)과 이메일 발송(NewsScheduler)이 서로 다른 결과를 보게 되는 문제를 막기 위해,
 * "기사 목록"과 "요약"을 절대 따로 캐시하지 않고 항상 같이 저장/조회한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DigestService {

    private final NewsService newsService;
    private final AiService aiService;

    private static final long CACHE_TTL = 1000 * 60 * 30; // 30분

    public static class Digest {
        /** title/link/description/pubDate/summary(null 가능) 를 담은 기사 목록 */
        public final List<Map<String, String>> articles;
        public final long fetchedAt;

        public Digest(List<Map<String, String>> articles) {
            this.articles = articles;
            this.fetchedAt = System.currentTimeMillis();
        }

        public boolean isEmpty() {
            return articles.isEmpty();
        }

        /** 새 기사가 있는지 비교하기 위한 최신 기사 링크 (없으면 null) */
        public String latestLink() {
            return articles.isEmpty() ? null : articles.get(0).get("link");
        }
    }

    private final Map<String, Digest> cache = new ConcurrentHashMap<>();

    public Digest getDigest(String keyword) {
        Digest cached = cache.get(keyword);
        if (cached != null && System.currentTimeMillis() - cached.fetchedAt < CACHE_TTL) {
            log.info("다이제스트 캐시 히트: {}", keyword);
            return cached;
        }

        List<Map<String, String>> articles = newsService.fetchNews(keyword);
        if (!articles.isEmpty()) {
            List<String> summaries = aiService.summarizeEach(keyword, articles);
            for (int i = 0; i < articles.size(); i++) {
                String s = i < summaries.size() ? summaries.get(i) : null;
                // 실패(null)이면 요약 없이 제목/링크만 노출 (실패 문구를 요약처럼 보내지 않음)
                articles.get(i).put("summary", s);
            }
        }

        Digest digest = new Digest(articles);
        cache.put(keyword, digest);
        return digest;
    }
}
