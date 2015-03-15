# Upgrade Procedure #

## Upgrading from version 0.2.0 to 0.3.0 ##
The database schema for annotation document and source document is modified to include the **state** of the document.
To upgrade your existing application from 0.2.0 to 0.3.0
  1. Start your application server with the new webanno.war file
  1. run the following commands in mysql:
```
USE webanno;
UPDATE `source_document`  SET `state`='NEW';
UPDATE `annotation_document` SET `state`='INPROGRESS';
UPDATE `source_document` SET `format`='tcf' WHERE `format`='Weblicht TCF Format';
UPDATE `source_document` SET `format`='text' WHERE `format`='Plain text';
UPDATE `source_document` SET `format`='xmi' WHERE `format`='XMI format';
UPDATE `source_document` SET `format`='conll' WHERE `format`='Conll Format';
```
  1. Restart your application

## Copying data and metadata from version 0.2.0 to 0.3.0 ##
If you have annotation data and metadata in 0.2.0 and want to migrate to 0.3.0, do the following steps

  * Copy database to new 0.3.0 instance
  1. create the database for 0.3.0
```
mysql> create database webanno
```
  1. start webanno
  1. create mysqldump from 0.2.0
```
mysqldump -u root -p webanno > webanno.sql
```
  1. restore the mysqldump data to 0.3.0 webanno database
```
mysql -u root -p  webanno < webanno.sql
```
  1. make updates appropraitely as stated above (UPDATE `webanno`.....)
  1. restart webanno


  * copy database to existing 0.3.0 database
  1. create mysqldump only for data
```
mysqldump -u root -p -t -c webanno > webanno.sql
```
  1. restore only the data to the existing database
```
mysql -u root -p -f webanno < webanno.sql
```

  1. make updates appropraitely as stated above (UPDATE `webanno`.....)
  1. restart webanno
## Upgrading from version 0.3.0 to 0.4.0 ##

Before starting using the 0.4.0 war file make the following changes to the authorities schema as follows:
  1. change the role column as authority as follows
```
>use webanno;
>alter table `authorities`  change `role` `authority` VARCHAR(255) NOT NULL ;
```
  1. remove the foreign key constraint of user column. to do so, get the name of the constraint as follows:
```
>use webanno;
>SHOW CREATE TABLE authorities;
```
This will display the name of the foreign key constarint, something simillar as
```
authorities | CREATE TABLE `authorities` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `role` varchar(255) NOT NULL,
  `user` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `role` (`role`,`user`),
  KEY `FK2B0F1321393B7DBE` (`user`),
  CONSTRAINT `FK2B0F1321393B7DBE` FOREIGN KEY (`user`) REFERENCES `users` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=50 DEFAULT CHARSET=latin1 
```
  1. remove the foreign key constariant as follows
```
>alter table `authorities` drop foreign key `FK2B0F1321393B7DBE`;
```
  1. change user column as follows:
```
>alter table `authorities`  change `user` `username` VARCHAR(255) NOT NULL ;
```
  1. add the foreign key constraint back using the following command

```
>alter table `authorities` add foreign key (`username`) references `users` (`username`);
```
  * in 0.4.0, the conll reader/writer is changed to tsv reader/writer. in the /srv/webanno/formats.properties file make the following changes
<---
```
conll.label=Conll Format
conll.reader=de.tudarmstadt.ukp.clarin.webanno.conll.ConllReader
conll.writer=de.tudarmstadt.ukp.clarin.webanno.conll.ConllWriter
```
-->
```
tsv.label=TSV Format
tsv.reader=de.tudarmstadt.ukp.clarin.webanno.tsv.WebannoTsvReader
tsv.writer=de.tudarmstadt.ukp.clarin.webanno.tsv.WebannoTsvWriter
```

  * change the database entry of source\_documents table as follows
```
>USE webanno;
>UPDATE source_document SET `format`='tsv' where `format`='conll';
```

  * since 0.4.0, projects have modes either as ANNOTATION or CORRECTION. **Restart webbanno** and **then only** update the project table as follows
```
UPDATE project SET `mode`='annotation' where `mode` is NULL;
```

  1. If the username admin is not present, create it as follows:
```
INSERT INTO `users` (`username`, `enabled`, `password`) VALUES ('admin', '1', 'b7f76f3d1cfa8746e9d44a9ccca81bf22e0e123f06519fc964f97172d5d61b731e9bf8a11da4a3d0');
```
  1. Update the authorities table with ROLE\_ADMIN for user admin as follows.
```
INSERT INTO `authorities` (`authority`, `username`) VALUES ('ROLE_ADMIN', 'admin');

```
  1. If the user name admin is already there, update the old password with a hash as follows:
```
UPDATE `users` SET `password`='b7f76f3d1cfa8746e9d44a9ccca81bf22e0e123f06519fc964f97172d5d61b731e9bf8a11da4a3d0' WHERE `username`='admin';
```
  1. Log in to webanno using admin/admin
  1. Go to **manage users** and update passwords for the remaining users. Make sure that ROLE\_USER is selected for every user. Multiple roles can be selected by selecting using **ctrl + click**

## Upgrading from version 0.4.0 to 0.5.0 ##
  1. If you need to have case sensitive tag, such as Loc, LOC..., change collation of tag table as follows:
```
>use webanno;
> ALTER TABLE tag  CONVERT TO CHARACTER SET utf8 COLLATE 'utf8_bin';
```
  1. The column **reverseDependencyDirection** in the project table is no more needed in the 0.5.0+ releases. If you have an old project, remove the column as follows
```
>use webanno;
> ALTER TABLE project DROP COLUMN `reverseDependencyDirection` ;
```