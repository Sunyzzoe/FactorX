package com.factorx.backtest;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Profile("postgres")
public class ModelService {
    private static final List<String> FEATURES = List.of("final_impact_score", "relevance_score", "relu_score", "direction");
    private final BacktestService backtestService;
    private final Map<String, Model> models = new ConcurrentHashMap<>();

    public ModelService(BacktestService backtestService) { this.backtestService = backtestService; }

    public Map<String, Object> train(TrainRequest request) {
        int horizon = targetHorizon(request.target());
        Map<String, Object> raw = backtestService.backtest(new BacktestRequest(
                request.startDate(), request.endDate(), List.of(), List.of(horizon), 0d, 0d));
        List<?> rawSamples = (List<?>) raw.get("samples");
        List<double[]> x = new ArrayList<>(); List<Integer> y = new ArrayList<>();
        for (Object item : rawSamples) {
            Map<?, ?> sample = (Map<?, ?>) item;
            Map<?, ?> returns = (Map<?, ?>) sample.get("returns");
            Object returnValue = returns.get(horizon) == null ? returns.get(String.valueOf(horizon)) : returns.get(horizon);
            if (returnValue == null) continue;
            double direction = "利空".equals(sample.get("direction")) ? -1 : 1;
            double signedReturn = direction * ((Number) returnValue).doubleValue();
            x.add(new double[]{1, number(sample.get("finalImpactScore")), number(sample.get("relevanceScore")), number(sample.get("reluScore")), direction});
            y.add(signedReturn > 0 ? 1 : 0);
        }
        if (x.size() < 5) throw new IllegalArgumentException("可训练样本不足，至少需要 5 条有效历史事件");
        double[] weights = fit(x, y);
        String version = (request.modelType() == null || request.modelType().isBlank() ? "logistic" : request.modelType())
                + "-" + horizon + "d-" + UUID.randomUUID().toString().substring(0, 8);
        Model model = new Model(version, request.target(), horizon, weights, x.size());
        models.put(version, model);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("modelVersion", version); result.put("modelType", "logistic-regression"); result.put("target", request.target());
        result.put("featureSchema", FEATURES); result.put("trainSamples", x.size()); result.put("weights", weights);
        result.put("directionAccuracy", accuracy(model, x, y));
        return result;
    }

    public Map<String, Object> predict(PredictRequest request) {
        Model model = models.get(request.modelVersion());
        if (model == null) throw new IllegalArgumentException("模型不存在或当前进程尚未加载: " + request.modelVersion());
        Map<String, Double> features = request.features() == null ? Map.of() : request.features();
        double direction = features.getOrDefault("direction", 1d);
        double probability = sigmoid(dot(model.weights, new double[]{1,
                features.getOrDefault("final_impact_score", 0d), features.getOrDefault("relevance_score", 0d),
                features.getOrDefault("relu_score", 0d), direction}));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("modelVersion", model.version); result.put("direction", probability >= 0.5 ? "up" : "down");
        result.put("probability", round(probability)); result.put("expectedReturn", null); result.put("target", model.target);
        return result;
    }

    private double[] fit(List<double[]> x, List<Integer> y) {
        double[] w = new double[5];
        for (int epoch = 0; epoch < 1800; epoch++) {
            double[] gradient = new double[5];
            for (int i = 0; i < x.size(); i++) { double error = sigmoid(dot(w, x.get(i))) - y.get(i); for (int j = 0; j < w.length; j++) gradient[j] += error * x.get(i)[j]; }
            for (int j = 0; j < w.length; j++) w[j] -= 0.08 * gradient[j] / x.size();
        }
        return w;
    }

    private double accuracy(Model model, List<double[]> x, List<Integer> y) {
        long correct = 0; for (int i = 0; i < x.size(); i++) if ((sigmoid(dot(model.weights, x.get(i))) >= 0.5 ? 1 : 0) == y.get(i)) correct++;
        return round((double) correct / x.size());
    }
    private int targetHorizon(String target) {
        if (target == null || !target.matches("(label_)?(1|3|5|10)(d)(_up|_return)?")) throw new IllegalArgumentException("target 仅支持 label_1d_up、label_3d_up、label_5d_return、label_10d_return");
        String number = target.replaceAll("[^0-9]", ""); return Integer.parseInt(number);
    }
    private double number(Object value) { return value instanceof Number n ? n.doubleValue() : 0; }
    private double dot(double[] a, double[] b) { double sum = 0; for (int i = 0; i < a.length; i++) sum += a[i] * b[i]; return sum; }
    private double sigmoid(double value) { if (value >= 0) { double z = Math.exp(-value); return 1 / (1 + z); } double z = Math.exp(value); return z / (1 + z); }
    private double round(double value) { return Math.round(value * 1000000d) / 1000000d; }
    private record Model(String version, String target, int horizon, double[] weights, int samples) { }
}
