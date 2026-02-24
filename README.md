# Bank System

This is simulate bank system to show concurrency and scheduler.

## Concurrency
```java
ExecutorService tellerPool = Executors.newFixedThreadPool(3);
```
สร้าง 3 threads ให้ทำงานพร้อมกัน
```txt
pool-1-thread-2 -> Executing [Transaction F] (Priority 1)
pool-1-thread-3 -> Executing [Transaction D] (Priority 2)
pool-1-thread-1 -> Executing [Transaction B] (Priority 1)
```
ส่วนนี้ของ Output มันอาจจะดูแปลกที่ทำไมถึงไปเอา Priority 2 มาก่อน Priority 1 แต่จริงๆแล้วมันคือ Thread ทั้ง 3 อัน มารับพร้อมกันแล้ว Thread 3 อาจจะเร็วกว่า Thread 1 เล็กน้อยทำให้มันเหมือนมาก่อนแต่จริงๆ มันมาพร้อมกัน

### มันโชว์ว่าทั้ง 3 threads ทำงานพร้อมกันจริง
## Deadlock prevention 
```java
BankAccount firstLock = this.accountId.compareTo(targetAccount.getId()) < 0 ? this : targetAccount;
BankAccount secondLock = this.accountId.compareTo(targetAccount.getId()) < 0 ? targetAccount : this;
```
ตรงนี้เป็นส่วนที่ป้องกัน deadlock ถ้ามี 2 task  ทำ transfer สวนกัน
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
ถ้ามีกัน deadlock ทั้ง 2 task จะ ไป lock Acc 1001 ก่อน
```
Thread A lock [Acc 1001]
Thread B lock [Acc 1001] but cannot need to wait
Thread A lock [Acc 1002]
Thread A finish unlock both Acc
Thread B start
No Dead lock
```
## Scheduler

```java
PriorityBlockingQueue<Transaction> queue = new PriorityBlockingQueue<>();
```
อันนี้คือ PriorityBlockingQueue อันนี้พิเศษกว่าคิวธรรมดาตรงที่ทุกครั้งที่เราใส่ข้อมูลไปใน PriorityBlockingQueue มันจะจัดลำดับให้ค่าที่น้อยที่สุดมาอยู่อันแรกเสมอ
```java
queue.put(new Transaction(1, "Task B", () -> acc1.transfer(acc2, 1000)));
```
ทุก Transaction จะมี Priority อยู่ 1 คือสำคัญสุด(ทำก่อน) 5 คือสำคัญน้อยสุด(ทำหลังสุด)
```java
queue.put(new Transaction(5, "Transaction A", () -> acc1.deposit(100)));
queue.put(new Transaction(1, "Transaction B", () -> acc1.transfer(acc2, 1000))); 
queue.put(new Transaction(3, "Transaction C", () -> acc3.withdraw(500)));
queue.put(new Transaction(2, "Transaction D", () -> acc2.transfer(acc3, 500)));
queue.put(new Transaction(4, "Transaction E", () -> acc1.withdraw(200)));
queue.put(new Transaction(1, "Transaction F", () -> acc3.transfer(acc1, 2000))); 
queue.put(new Transaction(5, "Transaction G", () -> acc2.deposit(300)));
queue.put(new Transaction(2, "Transaction H", () -> acc1.deposit(400)));
queue.put(new Transaction(4, "Transaction I", () -> acc3.deposit(100)));
queue.put(new Transaction(3, "Transaction J", () -> acc2.withdraw(100)));
```
```java
pool-1-thread-2 -> Executing [Transaction F] (Priority 1)
pool-1-thread-3 -> Executing [Transaction D] (Priority 2)
pool-1-thread-1 -> Executing [Transaction B] (Priority 1)
    pool-1-thread-2 [->] TRANSFERRED $2000.0 from ACC-1003 to ACC-1001
pool-1-thread-2 -> Executing [Transaction H] (Priority 2)
    pool-1-thread-3 [->] TRANSFERRED $500.0 from ACC-1002 to ACC-1003
pool-1-thread-3 -> Executing [Transaction J] (Priority 3)
    pool-1-thread-1 [->] TRANSFERRED $1000.0 from ACC-1001 to ACC-1002
pool-1-thread-1 -> Executing [Transaction C] (Priority 3)
    pool-1-thread-3 [-] Withdrew $100.0 from ACC-1002 | Bal: $2400.0
pool-1-thread-3 -> Executing [Transaction I] (Priority 4)
    pool-1-thread-1 [-] Withdrew $500.0 from ACC-1003 | Bal: $3000.0
    pool-1-thread-2 [+] Deposited $400.0 to ACC-1001 | Bal: $5400.0
    pool-1-thread-3 [+] Deposited $100.0 to ACC-1003 | Bal: $3100.0
pool-1-thread-1 -> Executing [Transaction E] (Priority 4)
    pool-1-thread-1 [-] Withdrew $200.0 from ACC-1001 | Bal: $5200.0
pool-1-thread-2 -> Executing [Transaction G] (Priority 5)
pool-1-thread-3 -> Executing [Transaction A] (Priority 5)
    pool-1-thread-2 [+] Deposited $300.0 to ACC-1002 | Bal: $2700.0
    pool-1-thread-3 [+] Deposited $100.0 to ACC-1001 | Bal: $5300.0
```
จาก Input ที่ใส่เข้าไปมันมั่ว Priority แต่ผลลัพท์ที่ออกมาคือจะทำ 1 ก่อนเสมอ 
### แบบนี้คือ __Priority Scheduling__

## Heap