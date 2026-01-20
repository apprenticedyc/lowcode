package com.hex.ailowcode.ai.tools;

import com.hex.ailowcode.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 文件写入工具
 * 通过 @Tool 注解提供给 AI Agent 调用，让 AI 能够生成并写入代码文件
 *
 * 核心功能：
 * 1. 接收文件相对路径、内容、应用ID三个参数
 * 2. 基于 appId 构建项目目录 vue_project_{appId}/
 * 3. 自动创建父目录，以覆盖模式写入文件
 * 4. 返回相对路径（避免泄露服务器内部路径）
 * 5. 异常处理：捕获 IOException，记录日志并返回错误信息
 */
@Slf4j
public class FileWriteTool {

    @Tool("写入文件到指定路径")
    public String writeFile(
            @P("文件的相对路径")
            String relativeFilePath,
            @P("要写入文件的内容")
            String content,
            @ToolMemoryId Long appId
    ) {
        try {
            // 1. 构建完整路径：CODE_OUTPUT_ROOT_DIR/vue_project_{appId}/ + relativeFilePath
            String projectDirName = "vue_project_" + appId;
            Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
            Path fullPath = projectRoot.resolve(relativeFilePath);

            // 2. 自动创建父目录（如果不存在）
            Path parentDir = fullPath.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }

            // 3. 以覆盖模式写入文件
            Files.write(fullPath, content.getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            log.info("成功写入文件: {}", fullPath.toAbsolutePath());

            // 4. 返回相对路径（避免泄露服务器内部路径）
            return "文件写入成功: " + relativeFilePath;

        } catch (IOException e) {
            String errorMessage = "文件写入失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }
}