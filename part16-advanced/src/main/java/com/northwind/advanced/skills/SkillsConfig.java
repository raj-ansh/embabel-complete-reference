package com.northwind.advanced.skills;

import com.embabel.agent.skills.EmbeddingSkillSelector;
import com.embabel.agent.skills.Skills;
import com.embabel.common.ai.model.EmbeddingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chapter 113: NorthWind's skills, loaded once from a directory of SKILL.md folders. Loading validates each
 * skill; a skill whose instructions mention a missing file is skipped with a warning.
 */
@Configuration
public class SkillsConfig {

    @Bean
    public Skills northwindSkills(@Value("${northwind.skills.directory}") String directory) {
        return new Skills("northwind", "NorthWind's writing and gear skills")
                .withLocalSkills(directory);
    }

    /** Reference skills marked "activation: embedding" are chosen by similarity, not by the model. */
    @Bean
    public EmbeddingSkillSelector skillSelector(EmbeddingService embeddings) {
        return new EmbeddingSkillSelector(embeddings);
    }
}
