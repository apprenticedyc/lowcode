# 开发环境配置说明

## 本地开发配置

1. **复制模板文件**
   ```bash
   cp src/main/resources/application-local-template.yaml src/main/resources/application-local.yaml
   ```

2. **设置环境变量**（推荐）
   ```bash
   # Windows
   set DB_USERNAME=root
   set DB_PASSWORD=你的密码
   set AI_API_KEY=你的API密钥
   set AI_BASE_URL=https://api.deepseek.com

   # Linux/Mac
   export DB_USERNAME=root
   export DB_PASSWORD=你的密码
   export AI_API_KEY=你的API密钥
   export AI_BASE_URL=https://api.deepseek.com
   ```

3. **或者直接编辑 application-local.yaml**
   （注意：不要提交到版本控制！）

## 安全提示

- ✅ `.gitignore` 已排除 `application-local.yaml`
- ✅ 模板文件使用环境变量占位符
- ✅ 敏感信息通过环境变量注入
- ⚠️ 开发时确保本地配置文件不包含真实密码