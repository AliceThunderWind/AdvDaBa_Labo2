package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Author {

    private String _id;
    private String name;

    public Map<String, String> toMap() {
        return Map.of(
                "_id", _id != null ? _id : UUID.randomUUID().toString(),
                "name", name != null ? name : "None");
    }

}
