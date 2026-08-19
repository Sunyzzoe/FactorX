import axios from "axios";
import type { AnalysisRequest, AnalysisResponse, NewsPage } from "../types/analysis";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export async function analyzeNews(payload: AnalysisRequest) {
  const response = await axios.post<AnalysisResponse>(`${API_BASE_URL}/api/analyze`, payload);
  return response.data;
}

export async function fetchNews(params: {
  page?: number;
  size?: number;
  source?: string;
  from?: string;
  to?: string;
} = {}) {
  const response = await axios.get<NewsPage>(`${API_BASE_URL}/api/news`, { params });
  return response.data;
}

export async function reanalyzeNews(id: number) {
  const response = await axios.post(`${API_BASE_URL}/api/news/${id}/reanalyze`);
  return response.data;
}
