import axios from "axios";
import type { AnalysisRequest, AnalysisResponse } from "../types/analysis";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export async function analyzeNews(payload: AnalysisRequest) {
  const response = await axios.post<AnalysisResponse>(`${API_BASE_URL}/api/analyze`, payload);
  return response.data;
}
