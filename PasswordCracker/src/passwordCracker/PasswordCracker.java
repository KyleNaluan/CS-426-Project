package passwordCracker;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.codec.digest.DigestUtils;

public class PasswordCracker {
  private Map<String, String> hashedPasswords;
  private List<String> dictionary;
  private Map<String, String> crackedPasswords = new HashMap<>();

  public PasswordCracker(String passwordsFile, String dictionaryFile) {
    this.hashedPasswords = loadHashedPasswords(passwordsFile);
    this.dictionary = loadDictionary(dictionaryFile);
  }

  private Map<String, String> loadHashedPasswords(String file) {
    FileReader reader;
    Map<String, String> hashes = new ConcurrentHashMap<>();

    try {
      reader = new FileReader(file);
      BufferedReader bufferedReader = new BufferedReader(reader);

      String line;

      while ((line = bufferedReader.readLine()) != null) {
        line = line.trim();
        String[] lineParts = line.split("\\s+");
        String userId = lineParts[0];
        String hash = lineParts[1];
        hashes.put(hash, userId);
      }

      bufferedReader.close();
      reader.close();

    } catch (IOException e) {
      e.printStackTrace();
    }

    return hashes;
  }

  private List<String> loadDictionary(String file) {
    FileReader reader;
    ArrayList<String> words = new ArrayList<String>();

    try {
      reader = new FileReader(file);
      BufferedReader bufferedReader = new BufferedReader(reader);

      String line;

      while ((line = bufferedReader.readLine()) != null) {
        line = line.trim();
        line = line.replace("\uFEFF", "");
        words.add(line);
      }

      bufferedReader.close();
      reader.close();

    } catch (IOException e) {
      e.printStackTrace();
    }

    return words;
  }

  public void crackPasswords() {
    ExecutorService executor = Executors.newFixedThreadPool(10);

    executor.submit(() -> {
      crackDigitPasswords();
    });

    executor.submit(() -> {
      crackSingleWordPasswords();
    });

    executor.submit(() -> {
      crackDoubleWordPasswords();
    });

    executor.submit(() -> {
      crackTripleWordPasswords();
    });

    executor.submit(() -> {
      crackSingleWordDigitPasswords();
    });

    executor.submit(() -> {
      crackDigitSingleWordPasswords();
    });

    executor.submit(() -> {
      crackDoubleWordDigitPasswords();
    });

    executor.submit(() -> {
      crackDigitDoubleWordPasswords();
    });

    executor.shutdown();

    try {
      while (!executor.awaitTermination(Long.MAX_VALUE, TimeUnit.HOURS)) {
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }

  }

  private List<String> generateNumbers(int maxLength) {
    List<String> numbers = new ArrayList<String>();
    for (int length = 1; length < maxLength; length++) {
      int maxNumber = (int) Math.pow(10, length);
      for (int num = 0; num < maxNumber; num++) {
        numbers.add(String.format("%0" + length + "d", num));
      }
    }

    return numbers;
  }

  private void crackDigitPasswords() {
    int maxLength = 10;
    for (int length = 1; length < maxLength; length++) {
      int maxNumber = (int) Math.pow(10, length);
      for (int num = 0; num < maxNumber; num++) {
        String password = String.format("%0" + length + "d", num);
        matchHash(password);
      }
    }
  }

  private void crackSingleWordPasswords() {
    for (String word : dictionary) {
      String password = word;
      matchHash(password);
    }
  }

  private void crackDoubleWordPasswords() {
    for (String word1 : dictionary) {
      for (String word2 : dictionary) {
        String password = word1 + word2;
        matchHash(password);
      }
    }
  }

  private void crackTripleWordPasswords() {
    for (String word1 : dictionary) {
      for (String word2 : dictionary) {
        for (String word3 : dictionary) {
          String password = word1 + word2 + word3;
          matchHash(password);
        }
      }
    }
  }

  private void crackSingleWordDigitPasswords() {
    int maxLength = 5;
    List<String> numbers = generateNumbers(maxLength);
    for (String word : dictionary) {
      for (String number : numbers) {
        String password = word + number;
        matchHash(password);
      }
    }
  }

  private void crackDigitSingleWordPasswords() {
    int maxLength = 3;
    List<String> numbers = generateNumbers(maxLength);
    for (String word : dictionary) {
      for (String number : numbers) {
        matchHash(number + word);
        String password = number + word;
        matchHash(password);
      }
    }
  }

  private void crackDoubleWordDigitPasswords() {
    int maxLength = 3;
    List<String> numbers = generateNumbers(maxLength);
    for (String word1 : dictionary) {
      for (String word2 : dictionary) {
        for (String number : numbers) {
          String password = word1 + word2 + number;
          matchHash(password);
        }
      }
    }
  }

  private void crackDigitDoubleWordPasswords() {
    int maxLength = 3;
    List<String> numbers = generateNumbers(maxLength);
    for (String word1 : dictionary) {
      for (String word2 : dictionary) {
        for (String number : numbers) {
          String password = number + word1 + word2;
          matchHash(password);
        }
      }
    }
  }

  private void matchHash(String password) {
    String hash = DigestUtils.sha1Hex(password);

    if (hashedPasswords.containsKey(hash)) {
      String userId = hashedPasswords.get(hash);
      crackedPasswords.put(userId, password);
      saveCrackedPassword("cracked_passwords.txt", userId, password);
      System.out.println("Cracked: User " + userId + " - " + password);
    }
  }

  private void saveCrackedPassword(String file, String userId, String password) {
    FileWriter crackedPasswordFile;

    try {
      crackedPasswordFile = new FileWriter(file);
      BufferedWriter writer = new BufferedWriter(crackedPasswordFile);
      writer.write("User " + userId + ": " + password);
      writer.newLine();
      writer.close();
      crackedPasswordFile.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  public static void main(String[] args) {
    PasswordCracker cracker = new PasswordCracker("passwords.txt", "dictionary.txt");
    cracker.crackPasswords();
  }
}