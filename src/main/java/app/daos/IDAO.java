package app.daos;

import app.exceptions.ApiException;

import java.util.List;

public interface IDAO<T, I> {

    T read(I i) throws ApiException;
    List<T> readAll() throws ApiException;
    T create(T t) throws ApiException;
    T update(I i, T t) throws ApiException;
    //void delete(I i) throws ApiException;
}
