package ru.gb.storage.common.message;

public class FileRequestMessage extends Message{
    private String path;
    private Boolean direction; // Направление передачи файла: true - запрос на чтение из сервера, false - запись на сервер

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Boolean getDirection() {
        return direction;
    }

    public void setDirection(Boolean direction) {
        this.direction = direction;
    }
}
