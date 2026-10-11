package com.northwind.llm.audit;

import com.embabel.agent.api.tool.callback.AfterToolResultContext;
import com.embabel.agent.api.tool.callback.ToolLoopTransformer;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Chapter 79: a transformer changes what the model sees. Email addresses in tool results are replaced
 * before the result enters the conversation, so the model can neither repeat nor leak them.
 */
@Component
public class EmailRedactingTransformer implements ToolLoopTransformer {

    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");

    @Override
    public String transformAfterToolResult(AfterToolResultContext context) {
        return EMAIL.matcher(context.getResultAsString()).replaceAll("[email removed]");
    }
}
