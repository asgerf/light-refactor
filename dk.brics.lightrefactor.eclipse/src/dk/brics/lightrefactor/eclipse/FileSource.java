package dk.brics.lightrefactor.eclipse;

import java.io.File;

import org.eclipse.core.resources.IFile;

import dk.brics.lightrefactor.ISource;

/**
 * Adapter for using an {@link IFile} from Eclipse together with an offset as an {@link ISource} in the refactoring framework.
 */
public class FileSource implements ISource {
  private IFile resource;
  private int offset;
  
  public FileSource(IFile resource) {
    this.resource = resource;
    this.offset = 0;
  }
  public FileSource(IFile resource, int offset) {
    this.resource = resource;
    this.offset = offset;
  }


  @Override
  public File getFile() {
    return (File) resource.getAdapter(File.class); // we don't use this anyway
  }
  public IFile getFileResource() {
    return resource;
  }
  public int getOffset() {
    return offset;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + offset;
    result = prime * result + ((resource == null) ? 0 : resource.hashCode());
    return result;
  }
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    FileSource other = (FileSource) obj;
    if (offset != other.offset)
      return false;
    if (resource == null) {
      if (other.resource != null)
        return false;
    } else if (!resource.equals(other.resource))
      return false;
    return true;
  }
  @Override
  public String toString() {
    return "FileSource [resource=" + resource + ", offset=" + offset + "]";
  }
}
