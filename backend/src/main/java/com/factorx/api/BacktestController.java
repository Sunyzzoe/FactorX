package com.factorx.api;

import com.factorx.backtest.BacktestRequest;
import com.factorx.backtest.BacktestService;
import com.factorx.backtest.ModelService;
import com.factorx.backtest.PredictRequest;
import com.factorx.backtest.TrainRequest;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
@Profile("postgres")
public class BacktestController {
    private final BacktestService backtestService;
    private final ModelService modelService;

    public BacktestController(BacktestService backtestService, ModelService modelService) {
        this.backtestService = backtestService;
        this.modelService = modelService;
    }

    @PostMapping("/backtests")
    public Map<String, Object> backtest(@Valid @RequestBody BacktestRequest request) { return backtestService.backtest(request); }

    @PostMapping("/train")
    public Map<String, Object> train(@Valid @RequestBody TrainRequest request) { return modelService.train(request); }

    @PostMapping("/predict")
    public Map<String, Object> predict(@Valid @RequestBody PredictRequest request) { return modelService.predict(request); }
}
