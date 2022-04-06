import mpi.MPI;

public class Main {

    //private final static Intracomm INTRACOMM = MPI.COMM_WORLD;

    public static void main(String[] args) {
        MPI.Init(args);
        int myRank = MPI.COMM_WORLD.Rank();
        int numberOfProcesses = MPI.COMM_WORLD.Size();
        if (myRank == 0) {
            new Philosopher(0, numberOfProcesses, new Fork(true, 0), new Fork(true, numberOfProcesses - 1));
        } else if (myRank == numberOfProcesses - 1) {
           new Philosopher(numberOfProcesses - 1, numberOfProcesses, new Fork(true, -1), new Fork(true, -1));
        } else {
            new Philosopher(myRank, numberOfProcesses, new Fork(true, myRank), new Fork(true, -1));
        }

    }
}
