# Bank System

This is simulate bank system to show concurrency and scheduler.

## Concurrency

```txt
pool-1-thread-2 -> Executing [Transaction F] (Priority 1)
pool-1-thread-3 -> Executing [Transaction D] (Priority 2)
pool-1-thread-1 -> Executing [Transaction B] (Priority 1)
```
ส่วนนี้ของ Output มันอาจจะดูแปลกที่ทำไมถึงไปเอา Priority 2 มาก่อน Priority 1 แต่จริงๆแล้วมันคือ Thread ทั้ง 3 อัน มารับพร้อมกันแล้ว Thread 3 อาจจะเร็วกว่า Thread 1 เล็กน้อยทำให้มันเหมือนมาก่อนแต่จริงๆ มันมาพร้อมกัน

## Deadlock prevention 
```java
BankAccount firstLock = this.accountId.compareTo(targetAccount.getId()) < 0 ? this : targetAccount;
BankAccount secondLock = this.accountId.compareTo(targetAccount.getId()) < 0 ? targetAccount : this;
```
ตรงนี้เป็นส่วนที่ป้องกัน deadlock คือถ้ามี 2 task  ทำ transfer สวนกันแบบ
```
Thread A [ Transferring Acc 1001 --> Acc 1002]
Thread B [ Transferring Acc 1002 --> Acc 1001]  
```
ถ้าไม่มีกัน deadlock สิ่งที่มันจะเกิดขึ้นคือ
```
Thread A lock [Acc 1001]
Thread B lock [Acc 1002]
Thread A want to edit [Acc 1002] but it lock by B
Thread B want to edit [Acc 1001] but it lock by A
So it wait for each other to unlock.
BOOM!! Deadlock
```
ถ้ามี ทั้ง 2 task จะ ไป lock Acc 1001 ก่อน
```
Thread A lock [Acc 1001]
Thread B lock [Acc 1001] but cannot need to wait
Thread A lock [Acc 1002]
Thread A finish unlock both Acc
Thread B start
No Dead lock
```
