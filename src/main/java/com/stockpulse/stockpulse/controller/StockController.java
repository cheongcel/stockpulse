package com.stockpulse.stockpulse.controller;

import com.stockpulse.stockpulse.domain.Stock;
import com.stockpulse.stockpulse.repository.StockRepository;
import com.stockpulse.stockpulse.service.AiService;
import com.stockpulse.stockpulse.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class StockController {

    private final StockRepository stockRepository;
    private final NewsService newsService;
    private final AiService aiService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("stocks", stockRepository.findAll());
        return "index";
    }

    @PostMapping("/subscribe")
    public String subscribe(@RequestParam String keyword,
                            @RequestParam String email,
                            RedirectAttributes redirectAttributes) {
        if (!stockRepository.existsByKeyword(keyword)) {
            stockRepository.save(new Stock(keyword, email));
            redirectAttributes.addFlashAttribute("successMsg",
                    keyword + " 키워드 구독이 완료됐어요!");
        } else {
            redirectAttributes.addFlashAttribute("successMsg",
                    "이미 구독 중인 키워드예요.");
        }
        return "redirect:/";
    }
    @PostMapping("/unsubscribe/{keyword}")
    public String unsubscribe(@PathVariable String keyword,
                              RedirectAttributes redirectAttributes) {
        stockRepository.findByKeyword(keyword).ifPresent(stockRepository::delete);
        redirectAttributes.addFlashAttribute("successMsg", keyword + " 구독이 취소됐어요.");
        return "redirect:/";
    }

    @GetMapping("/analyze/{keyword}")
    public String analyze(@PathVariable String keyword, Model model) {
        List<Map<String, String>> newsList = newsService.fetchNews(keyword);
        List<String> titles = newsService.extractTitles(newsList);
        String aiResult = aiService.analyzeNews(keyword, titles);

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node =
                    mapper.readTree(aiResult);

            model.addAttribute("summary",
                    node.has("summary") ? node.get("summary").asText() : "요약 없음");
            model.addAttribute("sentiment",
                    node.has("sentiment") ? node.get("sentiment").asText() : "NEUTRAL");
            model.addAttribute("score",
                    node.has("score") ? node.get("score").asDouble() : 0.5);
        } catch (Exception e) {
            model.addAttribute("summary", "분석 중 오류가 발생했어요. 다시 시도해주세요.");
            model.addAttribute("sentiment", "NEUTRAL");
            model.addAttribute("score", 0.5);
        }

        model.addAttribute("keyword", keyword);
        model.addAttribute("newsList", newsList);
        return "result";
    }
}