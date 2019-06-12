# update-fusion-user-password
Updates the user password for fusion


## Usage

```
java -jar update-fusion-user-password-1.0.jar -zkConnect localhost:9983 -fusionVersion 4.1.0 -username admin
```

It will ask you to enter your password, then confirm, then it will update the user's password. 

# Building

```
./gradlew clean jar
```
