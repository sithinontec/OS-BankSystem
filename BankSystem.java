import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

// 1. Abstract Class with Concurrency Control & Deadlock Prevention
abstract class BankAccount {
    protected String accountId;
    protected double balance;
    protected final ReentrantLock lock;

    public BankAccount(String accountId, double initialBalance) {
        this.accountId = accountId;
        this.balance = initialBalance;
        this.lock = new ReentrantLock();
    }

    public String getId() {
        return accountId;
    }

    public void deposit(double amount) {
        lock.lock();
        try {
            balance += amount;
            System.out.println("    " + Thread.currentThread().getName() + " [+] Deposited $" + amount + " to " + accountId + " | Bal: $" + balance);
        } finally {
            lock.unlock();
        }
    }

    public boolean withdraw(double amount) {
        lock.lock();
        try {
            if (balance >= amount) {
                balance -= amount;
                System.out.println("    " + Thread.currentThread().getName() + " [-] Withdrew $" + amount + " from " + accountId + " | Bal: $" + balance);
                return true;
            } else {
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    // Thread-safe transfer with Deadlock Prevention
    public boolean transfer(BankAccount targetAccount, double amount) {
        BankAccount firstLock = this.accountId.compareTo(targetAccount.getId()) < 0 ? this : targetAccount;
        BankAccount secondLock = this.accountId.compareTo(targetAccount.getId()) < 0 ? targetAccount : this;

        firstLock.lock.lock();
        try {
            secondLock.lock.lock();
            try {
                if (this.balance >= amount) {
                    this.balance -= amount;
                    targetAccount.balance += amount;
                    System.out.println("    " + Thread.currentThread().getName() + " [->] TRANSFERRED $" + amount + " from " + this.accountId + " to " + targetAccount.getId());
                    return true;
                } else {
                    return false;
                }
            } finally {
                secondLock.lock.unlock();
            }
        } finally {
            firstLock.lock.unlock();
        }
    }

    public double getBalance() {
        lock.lock();
        try {
            return balance;
        } finally {
            lock.unlock();
        }
    }
}

// 2. Concrete Implementation
class SavingsAccount extends BankAccount {
    public SavingsAccount(String accountId, double initialBalance) {
        super(accountId, initialBalance);
    }
}

// 3. PRIORITY TASK WRAPPER
class PriorityTask implements Runnable, Comparable<PriorityTask> {
    private final int priority;
    private final String taskName;
    private final Runnable action;

    public PriorityTask(int priority, String taskName, Runnable action) {
        this.priority = priority;
        this.taskName = taskName;
        this.action = action;
    }

    @Override
    public void run() {
        // RESTORED: This line explicitly shows which priority is being pulled by which thread!
        System.out.println(Thread.currentThread().getName() + " -> Executing [" + taskName + "] (Priority " + priority + ")");
        action.run(); 
    }

    @Override
    public int compareTo(PriorityTask other) {
        return Integer.compare(this.priority, other.priority);
    }
}

// 4. Main System - CONCURRENCY + PRIORITY
public class BankSystem {
    public static void main(String[] args) {
        System.out.println("--- Booting Bank System ---");
        
        SavingsAccount acc1 = new SavingsAccount("ACC-1001", 4000); // Alice
        SavingsAccount acc2 = new SavingsAccount("ACC-1002", 2000); // Bob
        SavingsAccount acc3 = new SavingsAccount("ACC-1003", 5000); // Charlie

        PriorityBlockingQueue<PriorityTask> queue = new PriorityBlockingQueue<>();

        // Load up 10 transactions into the queue
        // Prio 1: Highest (Urgent) | Prio 5: Lowest (Standard)
        queue.put(new PriorityTask(5, "Task A", () -> acc1.deposit(100)));
        queue.put(new PriorityTask(1, "Task B", () -> acc1.transfer(acc2, 1000))); // Urgent Transfer!
        queue.put(new PriorityTask(3, "Task C", () -> acc3.withdraw(500)));
        queue.put(new PriorityTask(2, "Task D", () -> acc2.transfer(acc3, 500)));
        queue.put(new PriorityTask(4, "Task E", () -> acc1.withdraw(200)));
        queue.put(new PriorityTask(1, "Task F", () -> acc3.transfer(acc1, 2000))); // Urgent Transfer!
        queue.put(new PriorityTask(5, "Task G", () -> acc2.deposit(300)));
        queue.put(new PriorityTask(2, "Task H", () -> acc1.deposit(400)));
        queue.put(new PriorityTask(4, "Task I", () -> acc3.deposit(100)));
        queue.put(new PriorityTask(3, "Task J", () -> acc2.withdraw(100)));

        System.out.println("--- Queued 10 transactions. Spinning up 3 Bank Tellers (Threads) ---\n");

        // Create a pool of 3 threads (Tellers)
        ExecutorService tellerPool = Executors.newFixedThreadPool(3);

        for (int i = 0; i < 3; i++) {
            tellerPool.execute(() -> {
                // Keep grabbing tasks until the queue is completely empty
                while (true) {
                    PriorityTask task = queue.poll(); 
                    if (task == null) break; 
                    task.run();
                }
            });
        }

        // Tell the pool we are done assigning work, and wait for them to finish
        tellerPool.shutdown();
        try {
            tellerPool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.out.println("Tellers were interrupted!");
        }

        // Print final balances
        System.out.println("\n--- System Shutdown --- Final Balances:");
        System.out.println("ACC-1001: $" + acc1.getBalance()); 
        System.out.println("ACC-1002: $" + acc2.getBalance()); 
        System.out.println("ACC-1003: $" + acc3.getBalance()); 
    }
}