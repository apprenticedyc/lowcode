package com.hex.ailowcode.core;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.hex.ailowcode.ai.model.HtmlCodeResult;
import com.hex.ailowcode.ai.model.MultiFileCodeResult;
import com.hex.ailowcode.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 文件写入工具类
 * 负责将 AI 生成的代码（HTML/CSS/JS）持久化到本地文件系统
 *  1. 保存目录 - 使用固定的根目录 tmp/code_output
 *  2. 两个核心方法：
 *     - saveHtmlCodeResult() - 保存单文件 HTML 结果，生成 index.html
 *     - saveMultiFileCodeResult() - 保存多文件结果，生成 index.html、style.css、script.js
 *  3. 子目录命名策略 - 使用雪花 ID 保证唯一性，格式为 {业务类型}_{雪花ID}（如 html_1234567890）
 *  4. 依赖工具 - 使用 Hutool 的 FileUtil 和 IdUtil 简化文件操作和 ID 生成
 */
public class CodeFileSaver {

    // 文件保存根目录
    private static final String FILE_SAVE_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 保存 HtmlCodeResult
     */
    public static File saveHtmlCodeResult(HtmlCodeResult result) {
        String baseDirPath = buildUniqueDir(CodeGenTypeEnum.HTML.getValue());
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
        return new File(baseDirPath);
    }

    /**
     * 保存 MultiFileCodeResult
     */
    public static File saveMultiFileCodeResult(MultiFileCodeResult result) {
        String baseDirPath = buildUniqueDir(CodeGenTypeEnum.MULTI_FILE.getValue());
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
        writeToFile(baseDirPath, "style.css", result.getCssCode());
        writeToFile(baseDirPath, "script.js", result.getJsCode());
        return new File(baseDirPath);
    }

    /**
     * 构建唯一目录路径：tmp/code_output/bizType_雪花ID
     */
    private static String buildUniqueDir(String bizType) {
        String uniqueDirName = StrUtil.format("{}_{}", bizType, IdUtil.getSnowflakeNextIdStr());
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     * 写入单个文件
     */
    private static void writeToFile(String dirPath, String filename, String content) {
        String filePath = dirPath + File.separator + filename;
        FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
    }
}
