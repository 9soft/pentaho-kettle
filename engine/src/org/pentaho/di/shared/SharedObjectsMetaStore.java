package org.pentaho.di.shared;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.metastore.DatabaseMetaStoreUtil;
import org.pentaho.metastore.api.IMetaStore;
import org.pentaho.metastore.api.IMetaStoreElement;
import org.pentaho.metastore.api.IMetaStoreElementType;
import org.pentaho.metastore.api.exceptions.MetaStoreDependenciesExistsException;
import org.pentaho.metastore.api.exceptions.MetaStoreElementExistException;
import org.pentaho.metastore.api.exceptions.MetaStoreElementTypeExistsException;
import org.pentaho.metastore.api.exceptions.MetaStoreException;
import org.pentaho.metastore.api.exceptions.MetaStoreNamespaceExistsException;
import org.pentaho.metastore.stores.memory.MemoryMetaStore;
import org.pentaho.metastore.util.PentahoDefaults;

public class SharedObjectsMetaStore extends MemoryMetaStore implements IMetaStore {
  
  protected IMetaStoreElementType databaseElementType;

  protected SharedObjects sharedObjects;

  public SharedObjectsMetaStore(SharedObjects sharedObjects) throws MetaStoreException {
    this.sharedObjects = sharedObjects;
    
    this.databaseElementType = DatabaseMetaStoreUtil.populateDatabaseElementType(this);
  }
  
  @Override
  public List<String> getNamespaces() throws MetaStoreException {
    return Arrays.asList( PentahoDefaults.NAMESPACE );
  }

  @Override
  public void createNamespace(String namespace) throws MetaStoreException, MetaStoreNamespaceExistsException {
    throw new MetaStoreException("The shared objects metadata store doesn't support creating namespaces");
  }

  @Override
  public void deleteNamespace(String namespace) throws MetaStoreException, MetaStoreDependenciesExistsException {
    throw new MetaStoreException("The shared objects metadata store doesn't support deleting namespaces");
  }
  
  @Override
  public boolean namespaceExists(String namespace) throws MetaStoreException {
    return getNamespaces().indexOf(namespace)>=0;
  }

  @Override
  public List<IMetaStoreElementType> getElementTypes(String namespace) throws MetaStoreException {
    return Arrays.asList( databaseElementType  );
  }

  @Override
  public List<String> getElementTypeIds(String namespace) throws MetaStoreException {
    return Arrays.asList( databaseElementType.getId() );
  }

  @Override
  public IMetaStoreElementType getElementType(String namespace, String elementTypeId) throws MetaStoreException {
    if (elementTypeId.equals(databaseElementType.getId())) {
      return databaseElementType;
    }
    return null;
  }

  @Override
  public IMetaStoreElementType getElementTypeByName(String namespace, String elementTypeName) throws MetaStoreException {
    for (IMetaStoreElementType elementType : getElementTypes(namespace)) {
      if (elementType.getName()!=null && elementType.getName().equalsIgnoreCase(elementTypeName)) {
        return elementType;
      }
    }
    return null;
  }

  @Override
  public void createElementType(String namespace, IMetaStoreElementType elementType) throws MetaStoreException, MetaStoreElementTypeExistsException {
    throw new MetaStoreException("The shared objects metadata store doesn't support creating new element types");
  }

  @Override
  public void updateElementType(String namespace, IMetaStoreElementType elementType) throws MetaStoreException {
    throw new MetaStoreException("The shared objects metadata store doesn't support updating element types");
  }

  @Override
  public void deleteElementType(String namespace, String elementTypeId) throws MetaStoreException, MetaStoreDependenciesExistsException {
    throw new MetaStoreException("The shared objects metadata store doesn't support deleting element types");
  }

  @Override
  public List<IMetaStoreElement> getElements(String namespace, String elementTypeId) throws MetaStoreException {
    List<IMetaStoreElement> list = new ArrayList<IMetaStoreElement>();
    for (SharedObjectInterface sharedObject : sharedObjects.getObjectsMap().values()) {
      // The databases...
      //
      if (sharedObject instanceof DatabaseMeta && databaseElementType.getId().equals(elementTypeId)) {
        list.add( DatabaseMetaStoreUtil.populateDatabaseElement(this, (DatabaseMeta)sharedObject) );
      }
    }
    return list;
  }

  @Override
  public List<String> getElementIds(String namespace, String elementTypeId) throws MetaStoreException {
    List<String> ids = new ArrayList<String>();
    for (IMetaStoreElement element : getElements(namespace, elementTypeId)) {
      ids.add(element.getId());
    }
    return ids;
  }

  @Override
  public IMetaStoreElement getElement(String namespace, String elementTypeId, String elementId) throws MetaStoreException {
    for (IMetaStoreElement element : getElements(namespace, elementTypeId)) {
      if (element.getId().equals(elementId)) {
        return element;
      }
    }
    return null;
  }

  @Override
  public IMetaStoreElement getElementByName(String namespace, IMetaStoreElementType elementType, String name) throws MetaStoreException {
    for (IMetaStoreElement element : getElements(namespace, elementType.getId())) {
      if ((element.getName().equalsIgnoreCase(name))) {
        return element;
      }
    }
    return null;
  }

  @Override
  public void createElement(String namespace, String elementTypeId, IMetaStoreElement element) throws MetaStoreException, MetaStoreElementExistException {
    try {
      IMetaStoreElement exists = getElement(namespace, elementTypeId, element.getId());
      if (exists!=null) {
        throw new MetaStoreException("The shared objects meta store already contains an element with type id '"+elementTypeId+"' and element id '"+element.getId());
      }
  
      if (elementTypeId.equals(databaseElementType.getId())) {
        // convert the element to DatabaseMeta and store it in the shared objects file, then save the file
        //
        sharedObjects.storeObject(DatabaseMetaStoreUtil.loadDatabaseMetaFromDatabaseElement(element));
        sharedObjects.saveToFile();
        return;
      }
      throw new MetaStoreException("Storing elements with element type id '"+elementTypeId+"' is not supported in the shared objects meta store");
    } catch(Exception e) {
      throw new MetaStoreException("Unexpected error creating an element in the shared objects meta store", e);
    }
  }

  @Override
  public void deleteElement(String namespace, String elementTypeId, String elementId) throws MetaStoreException {
    try {
      if (elementTypeId.equals(databaseElementType.getId())) {
        sharedObjects.removeObject(DatabaseMetaStoreUtil.loadDatabaseMetaFromDatabaseElement(getElement(namespace, elementTypeId, elementId)));
        sharedObjects.saveToFile();
        return;
      }
    } catch(Exception e) {
      throw new MetaStoreException("Unexpected error deleting an element in the shared objects meta store", e);
    }
  }

  public SharedObjects getSharedObjects() {
    return sharedObjects;
  }

  public void setSharedObjects(SharedObjects sharedObjects) {
    this.sharedObjects = sharedObjects;
  }


}
