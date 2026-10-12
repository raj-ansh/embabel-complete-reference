package com.northwind.tenancy.knowledge;

import com.embabel.agent.filter.PropertyFilter;
import com.embabel.agent.rag.ingestion.ContentChunker;
import com.embabel.agent.rag.ingestion.TikaHierarchicalContentReader;
import com.embabel.agent.rag.ingestion.transform.ChainedChunkTransformer;
import com.embabel.agent.rag.lucene.LuceneSearchOperations;
import com.embabel.agent.rag.tools.ToolishRag;
import com.northwind.tenancy.tenant.TenantContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chapter 105: one index shared by all tenants. Every chunk carries its tenant, and every search the model
 * can run is filtered by the tenant of the request, in code. The model can't widen the filter: it never sees
 * it, and the tool it is given only searches one tenant's documents.
 */
@Component
public class TenantKnowledge implements AutoCloseable {

    private static final Pattern TENANT_URI = Pattern.compile("^northwind://tenants/([a-z0-9-]+)/");

    private final LuceneSearchOperations store = LuceneSearchOperations.withName("tenant-knowledge")
            .withEmbeddingService(new HashingEmbeddingService())
            .withChunkerConfig(new ContentChunker.Config(800, 100, 32))
            .withChunkTransformer(new ChainedChunkTransformer(List.of(new TenantChunkTransformer())))
            .build();
    private final TikaHierarchicalContentReader reader = new TikaHierarchicalContentReader();

    static String tenantOf(String uri) {
        Matcher m = TENANT_URI.matcher(uri);
        if (!m.find()) {
            throw new IllegalArgumentException("Not a tenant document: " + uri);    // never ingest unowned content
        }
        return m.group(1);
    }

    /** Loads every tenant's documents from classpath:docs/{tenant}/**.md. */
    public List<String> ingestAll() {
        List<String> uris = new ArrayList<>();
        try {
            for (Resource doc : new PathMatchingResourcePatternResolver().getResources("classpath:docs/**/*.md")) {
                String path = doc.getURL().toString().replace('\\', '/');
                String uri = "northwind://tenants/" + path.substring(path.lastIndexOf("/docs/") + "/docs/".length());
                try (InputStream in = doc.getInputStream()) {
                    store.writeAndChunkDocument(reader.parseContent(in, uri));
                }
                uris.add(uri);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return uris.stream().sorted().toList();
    }

    /** The only way the application gives a model access to knowledge: one tenant's customer documents. */
    public ToolishRag searchFor(TenantContext tenant) {
        return new ToolishRag("knowledge", tenant.tenantName() + " help articles and policies", store)
                .withMetadataFilter(PropertyFilter.Companion.and(
                        PropertyFilter.Companion.eq("tenant", tenant.tenantId()),
                        PropertyFilter.Companion.eq("audience", "customer")));
    }

    /** Unfiltered search, for the isolation tests that show why the filter matters. Never give this to a model. */
    ToolishRag unfiltered() {
        return new ToolishRag("knowledge", "All tenants' documents", store);
    }

    /** Chapter 107: onboarding adds a tenant's document. The URI decides the owner, so it is checked first. */
    public void ingest(String uri, String markdown) {
        tenantOf(uri);
        store.writeAndChunkDocument(reader.parseContent(
                new ByteArrayInputStream(markdown.getBytes(StandardCharsets.UTF_8)), uri));
    }

    /** Chapter 107: offboarding removes a tenant's documents. */
    public int deleteTenantDocument(String uri) {
        tenantOf(uri);
        return store.deleteRootAndDescendants(uri).getDeletedCount();
    }

    @Override
    public void close() {
        store.close();
    }
}
