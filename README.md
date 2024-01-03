# Apple II File System
## Purpose
This library is intended for other programs to use when needing access to the contents
 of Apple II disk images. It is currently used by [WizardryApp](https://github.com/dmolony/WizardryApp),
  and will be used by [DiskBrowser2](https://github.com/dmolony/DiskBrowser2).
## Usage
Create an instance of FileSystemFactory and then call getFileSystem() with either a File Path to a disk image, or an instance of BlockReader. This will return an
AppleFileSystem which can then be used to obtain any of the disk image's files or
blocks.