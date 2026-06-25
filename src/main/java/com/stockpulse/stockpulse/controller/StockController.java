package com.stockpulse.stockpulse.controller;

import com.stockpulse.stockpulse.domain.Stock;
import com.stockpulse.stockpulse.repository.StockRepository;
import com.stockpulse.stockpulse.service.AiService;
import com.stockpulse.stockpulse.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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
    public String subscribe(@RequestParam String ticker,
                            @RequestParam String companyName,
                            @RequestParam String email) {
        if (!stockRepository.existsByTicker(ticker)) {
            stockRepository.save(new Stock(ticker, companyName, email));
        }
        return "redirect:/";
    }

    @GetMapping("/analyze/{ticker}")
    public String analyze(@PathVariable String ticker, Model model) {
        List<String> titles = newsService.fetchNewsTitles(ticker);
        String aiResult = aiService.analyzeNews(ticker, titles);
        model.addAttribute("ticker", ticker);
        model.addAttribute("titles", titles);
        model.addAttribute("aiResult", aiResult);
        return "result";
    }
}