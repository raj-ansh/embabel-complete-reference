package com.northwind.docs.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.filter.PropertyFilter;
import com.embabel.agent.rag.lucene.LuceneSearchOperations;
import com.embabel.agent.rag.tools.ToolishRag;
import com.embabel.common.ai.model.LlmOptions;
import com.northwind.docs.agent.Docs.Asker;
import com.northwind.docs.agent.Docs.Audience;
import com.northwind.docs.agent.Docs.DocQuestion;
import com.northwind.docs.agent.Docs.GroundedAnswer;

/**
 * P7: answers questions from NorthWind's documents. The model searches (agentic RAG); the application
 * decides WHAT it may search: customers see public documents only, staff see everything.
 */
@Agent(description = "Answer questions about NorthWind's policies from the company's documents, with sources")
public class DocumentAssistantAgent {

    private final ToolishRag policies;

    public DocumentAssistantAgent(LuceneSearchOperations policyStore) {
        this.policies = new ToolishRag("policies", "NorthWind policy documents: returns, shipping, warranty, staff guides",
                policyStore);
    }

    /** The access rule lives in code. The model never sees the filter and cannot remove it. */
    ToolishRag scopedFor(Asker asker) {
        return asker.audience() == Audience.STAFF
                ? policies
                : policies.withMetadataFilter(PropertyFilter.Companion.eq("audience", "public"));
    }

    @AchievesGoal(description = "The question has been answered from the documents")
    @Action(description = "Search the documents and answer")
    public GroundedAnswer answer(DocQuestion question, Asker asker, OperationContext context) {
        ToolishRag rag = scopedFor(asker);
        return context.ai()
                .withLlm(LlmOptions.withLlmForRole("balanced").withTemperature(0.1))
                // Same as withReference(rag), minus duplicate tools: in 1.5.3 withReference offers each search tool
                // twice (policies_vectorSearch and policies_policies_vectorSearch). See Chapter 57.
                .withTools(rag.tools())
                .withPromptContributor(rag)
                .withSystemPrompt("""
                        You answer questions about NorthWind Outfitters using ONLY the policy documents you can search.
                        Search before answering; try different wording if the first search finds nothing useful.
                        Quote numbers exactly as the documents state them.
                        List the titles of the documents you used in sources.
                        If the documents do not answer the question, set answeredFromDocuments to false and say
                        you could not find it. Never answer from general knowledge.""")
                .createObject("""
                        <question>
                        %s
                        </question>""".formatted(question.question()), GroundedAnswer.class);
    }
}
