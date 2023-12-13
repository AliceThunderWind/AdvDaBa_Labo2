package org.example;

import com.google.gson.stream.JsonReader;
import org.example.model.Article;
import org.example.model.Author;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ArticleFactory {

    // Handle NumberInt() in a case if needed. Not asked in this practical work
    public static Article createArticle(JsonReader jsonReader) throws IOException {
        jsonReader.beginObject();
        Article article = new Article();

        while (jsonReader.hasNext()) {
            String key = jsonReader.nextName();
            switch (key) {
                case "_id":
                    article.set_id(jsonReader.nextString());
                    break;
                case "title":
                    article.setTitle(jsonReader.nextString());
                    break;
                case "authors":
                    article.setAuthors(createAuthors(jsonReader));
                    break;
                case "references":
                    article.setReferences(createReferences(jsonReader));
                    break;
                default:
                    jsonReader.skipValue();
                    break;
            }
        }
        jsonReader.endObject();
        return article;
    }

    private static Author createAuthor(JsonReader jsonReader) throws IOException {
        jsonReader.beginObject();
        Author author = new Author();

        while (jsonReader.hasNext()) {
            String key = jsonReader.nextName();
            switch(key) {
                case "_id":
                    author.set_id(jsonReader.nextString());
                    break;
                case "name":
                    author.setName(jsonReader.nextString());
                    break;
                default:
                    jsonReader.skipValue();
                    break;
            }
        }
        jsonReader.endObject();
        return author;
    }

    private static List<Author> createAuthors(JsonReader jsonReader) throws IOException {
        List<Author> authors = new ArrayList<>();
        jsonReader.beginArray();

        while (jsonReader.hasNext()) {
            authors.add(createAuthor(jsonReader));
        }

        jsonReader.endArray();
        return authors;
    }

    private static List<String> createReferences(JsonReader jsonReader) throws IOException {
        List<String> references = new ArrayList<>();
        jsonReader.beginArray();

        while (jsonReader.hasNext()) {
            references.add(jsonReader.nextString());
        }

        jsonReader.endArray();
        return references;
    }

}
