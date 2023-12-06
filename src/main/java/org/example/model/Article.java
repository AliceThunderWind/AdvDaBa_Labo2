package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class Article {

    private String _id;
    private String title;
    private List<Author> authors;
    private List<String> references;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("_id", _id != null ? _id : UUID.randomUUID().toString());
        map.put("title", title);
        map.put("authors", authorsToMap(authors));
        map.put("references", references);

        return map;
    }

    private List<Map<String, String>> authorsToMap(List<Author> authors) {
        if (authors == null || authors.isEmpty()) {
            return Collections.emptyList();
        }

        return authors.stream()
                .map(Author::toMap)
                .collect(Collectors.toList());
    }

}
