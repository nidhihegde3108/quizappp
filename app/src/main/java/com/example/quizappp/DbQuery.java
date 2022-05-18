package com.example.quizappp;

import android.util.ArrayMap;

import androidx.annotation.NonNull;

import com.example.quizappp.models.CategoryModel;
import com.example.quizappp.models.Profilemodel;
import com.example.quizappp.models.QuestionModel;
import com.example.quizappp.models.RankModel;
import com.example.quizappp.models.TestModel;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DbQuery {
   public static  FirebaseFirestore g_Firestore;
   public static List<CategoryModel> g_catList=new ArrayList<>();
   public static List<TestModel> g_testList=new ArrayList<>();
   public static Profilemodel myProfile=new Profilemodel("NA",null);
   public static int g_selected_cat_index=0;
   public static int g_selected_test_index=0;
   public static List<QuestionModel> g_quesList=new ArrayList<>();
   public static RankModel myPerformance=new RankModel(0,-1);
   public static final int NOT_VISITED=0;
   public static final int UNANSWERED=1;
   public static final int ANSWERED=2;
   public static final int REVIEW=3;

   public static void createUserData(String email, String name, MyCompleteListener completeListener)
   {
      Map<String,Object> userData=new ArrayMap<>();
      userData.put("EMAIL",email);
      userData.put("NAME",name);
      userData.put("TOTAL_SCORE",0);
      DocumentReference userDoc=g_Firestore.collection("USERS").document(FirebaseAuth.getInstance().getCurrentUser().getUid());
      WriteBatch batch=g_Firestore.batch();
      batch.set(userDoc,userData);

      DocumentReference countDoc=g_Firestore.collection("USERS").document("TOTAL_USERS");
      batch.update(countDoc,"COUNT", FieldValue.increment(1));
      batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
         @Override
         public void onSuccess(Void unused) {
            completeListener.onSuccess();
         }
      }).addOnFailureListener(new OnFailureListener() {
         @Override
         public void onFailure(@NonNull Exception e) {
            completeListener.onFailure();
         }
      });
   }
public static void saveResult(int score,MyCompleteListener completeListener)
{
   WriteBatch batch=g_Firestore.batch();
   DocumentReference userDoc=g_Firestore.collection("USERS").document(FirebaseAuth.getInstance().getUid());
   batch.update(userDoc,"TOTAL_SCORE",score);
   if(score > g_testList.get(g_selected_test_index).getTopScore())
   {
      DocumentReference scoreDoc=userDoc.collection("USER_DATA").document("MY_SCORES");
      batch.update(scoreDoc,g_testList.get(g_selected_test_index).getTestID(),score);
   }
   batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
      @Override
      public void onSuccess(Void unused) {
          if(score >g_testList.get(g_selected_test_index).getTopScore())
             g_testList.get(g_selected_test_index).getTopScore();
          myPerformance.setScore(score);
          completeListener.onSuccess();
      }

   }).addOnFailureListener(new OnFailureListener() {
      @Override
      public void onFailure(@NonNull Exception e) {
         completeListener.onFailure();
      }
   });
}

public static void getUserData(MyCompleteListener completeListener)
{
   g_Firestore.collection("USERS").document(FirebaseAuth.getInstance().getUid()).
           get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
      @Override
      public void onSuccess(DocumentSnapshot documentSnapshot) {
        myProfile.setName(documentSnapshot.getString("NAME"));
        myProfile.setEmail(documentSnapshot.getString("EMAIL_ID"));
        myPerformance.setScore(documentSnapshot.getLong("TOTAL_SCORE").intValue());
        completeListener.onSuccess();
      }
   }).addOnFailureListener(new OnFailureListener() {
      @Override
      public void onFailure(@NonNull Exception e) {
        completeListener.onFailure();
      }
   });
}
   public static void loadCategories(final MyCompleteListener completeListener)
   {
      g_catList.clear();
      g_Firestore.collection("QUIZ").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
         @Override
         public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
            Map<String, QueryDocumentSnapshot> docList=new ArrayMap<>();
            for(QueryDocumentSnapshot doc: queryDocumentSnapshots)
            {
               docList.put(doc.getId(),doc);
            }
            QueryDocumentSnapshot catListDooc=docList.get("Categories");
            long catCount=catListDooc.getLong("COUNT");
            for(int i=1;i<=catCount;i++)
            {
               String catID=catListDooc.getString("CAT"+String.valueOf(i)+"_ID");
               QueryDocumentSnapshot catDoc=docList.get(catID);
               int noOfTest=catDoc.getLong("NO_OF_TESTS").intValue();
               String catName=catDoc.getString("NAME");
               g_catList.add(new CategoryModel(catID,catName,noOfTest));

            }
            completeListener.onSuccess();
         }
      }).addOnFailureListener(new OnFailureListener() {
         @Override
         public void onFailure(@NonNull Exception e) {
               completeListener.onFailure();
         }
      });
   }
   public static void loadTestData(final MyCompleteListener completeListener)
   {
      g_testList.clear();
      g_Firestore.collection("QUIZ").document(g_catList.get(g_selected_cat_index).getDocID())
              .collection("TESTS_LIST").document("TESTS_INFO").get()
              .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                  @Override
                  public void onSuccess(DocumentSnapshot documentSnapshot) {
                     int noOfTests=g_catList.get(g_selected_cat_index).getNoOfTests();
                     for(int i=1;i<=noOfTests;i++)
                     {
                        g_testList.add(new TestModel(documentSnapshot.getString("TEST" + String.valueOf(i)+"_ID"),
                                0,documentSnapshot.getLong("TEST"+String.valueOf(i)+"_TIME").intValue()));
                     }
                      completeListener.onSuccess();
                  }
              })
              .addOnFailureListener(new OnFailureListener() {
                 @Override
                 public void onFailure(@NonNull Exception e) {
                    completeListener.onFailure();
                 }
              });

   }

   public static void loadData(MyCompleteListener completeListener)
   {
      loadCategories(new MyCompleteListener() {
         @Override
         public void onSuccess() {
            getUserData(completeListener);
         }

         @Override
         public void onFailure() {
           completeListener.onFailure();
         }
      });

   }

   public static void loadQuestions(MyCompleteListener completeListener)
   {
      g_quesList.clear();
      g_Firestore.collection("Questions")
              .whereEqualTo("CATEGORY",g_catList.get(g_selected_cat_index).getDocID())
              .whereEqualTo("TEST",g_testList.get(g_selected_test_index).getTestID())
              .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
         @Override
         public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
            for(DocumentSnapshot doc :queryDocumentSnapshots)
            {
               g_quesList.add(new QuestionModel(
                       doc.getString("QUESTION"),
                       doc.getString("A"),
                       doc.getString("B"),
                       doc.getString("C"),
                       doc.getString("D"),
                       doc.getLong("ANSWER").intValue(),-1,NOT_VISITED
               ));
            }
            completeListener.onSuccess();
         }
      }).addOnFailureListener(new OnFailureListener() {
         @Override
         public void onFailure(@NonNull Exception e) {
            completeListener.onFailure();
         }
      });
   }
}
