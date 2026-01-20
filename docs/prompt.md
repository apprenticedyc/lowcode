帮我根据分析需求、数据上报相关代码、示例从 Prometheus 收集到的数据，来生成 Grafana 看板的 JSON 导入代码，全部汇总到一个看板中。
相关的规范参考：https://grafana.com/docs/grafana/latest/dashboards/build-dashboards/view-dashboard-json-model/
## 需求
 成本分析
  - Token消耗趋势 - 按用户/应用/时间维度统计 Token 使用量，支持成本分摊
  - 人均Token消耗 - 识别高频用户，优化配额策略
  - 模型成本对比 - 不同模型的 Token 成本差异，指导模型选择
  性能分析
  - 响应时间分布 - P50/P95/P99 百分位数，识别长尾请求
  - 慢请求TOP榜 - 按用户/应用/模型排序，定位性能瓶颈
  - 高峰时段性能 - 不同时段的响应时间波动
  用户/应用维度
  - 活跃度排行 - 请求次数排名，识别核心用户/应用
  - 异常用户检测 - 短时间内大量请求或失败率异常
  - 应用健康度评分 - 综合请求量、成功率、响应时间评分
## 指标收集相关代码
monitor包下的相关代码, 比如
AiModelMetricsCollector.java
## 示例收集到的数据
HELP ai_model_requests_total AI模型总请求次数
TYPE ai_model_requests_total counter
ai_model_requests_total{app_id="313129227198590976",model_name="deepseek-chat",status="started",user_id="302588523967918080"} 2.0
ai_model_requests_total{app_id="313129227198590976",model_name="deepseek-chat",status="success",user_id="302588523967918080"} 2.0

HELP ai_model_response_duration_seconds AI模型响应时间
TYPE ai_model_response_duration_seconds summary
ai_model_response_duration_seconds_count{app_id="313129227198590976",model_name="deepseek-chat",user_id="302588523967918080"} 2
ai_model_response_duration_seconds_sum{app_id="313129227198590976",model_name="deepseek-chat",user_id="302588523967918080"} 91.285863

HELP ai_model_tokens_total AI模型Token消耗总数
TYPE ai_model_tokens_total counter
ai_model_tokens_total{app_id="313129227198590976",model_name="deepseek-chat",token_type="input",user_id="302588523967918080"} 1321.0
ai_model_tokens_total{app_id="313129227198590976",model_name="deepseek-chat",token_type="output",user_id="302588523967918080"} 519.0
ai_model_tokens_total{app_id="313129227198590976",model_name="deepseek-chat",token_type="total",user_id="302588523967918080"} 1840.0