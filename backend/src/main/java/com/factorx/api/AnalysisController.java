package com.factorx.api;

import com.factorx.model.AnalysisRequest;
import com.factorx.model.AnalysisResponse;
import com.factorx.model.ReluMomentumRequest;
import com.factorx.service.ReluFactorService;
import com.factorx.service.ReluResult;
import com.factorx.service.NewsAnalysisService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class AnalysisController {

    private final NewsAnalysisService newsAnalysisService;
    private final ReluFactorService reluFactorService;

    public AnalysisController(NewsAnalysisService newsAnalysisService, ReluFactorService reluFactorService) {
        this.newsAnalysisService = newsAnalysisService;
        this.reluFactorService = reluFactorService;
    }

    @GetMapping("/demo")
    public AnalysisResponse demo() {
        return newsAnalysisService.analyze(new AnalysisRequest(
                "Saudi Arabia announces $10B solar storage project involving Tesla suppliers",
                "Reuters",
                "Saudi Arabia has announced a $10 billion solar energy storage project. " +
                        "Tesla suppliers may participate in battery, inverter and grid storage deployment."
        ));
    }

    @PostMapping("/analyze")
    public AnalysisResponse analyze(@Valid @RequestBody AnalysisRequest request) {
        return newsAnalysisService.analyze(request);
    }

    @PostMapping("/relu-momentum")
    public ReluResult calculateReluMomentum(@Valid @RequestBody ReluMomentumRequest request) {
        return reluFactorService.calculate(request.closePrices(), request.threshold(), request.lookbackDays());
    }
}
