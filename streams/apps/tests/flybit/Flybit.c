/*

[Pipeline
  (Parentnull)
  [MethodDeclaration
    (modifiers public )
    (returns void)
    (name init)
    (parameters)
    [BlockStatement
      [SIRInitStatement
        (args)]
      [SIRInitStatement
        (args)]
      [SIRInitStatement
        (args)]]]]
[Filter
  (Parent)
  [FieldDeclaration
    (modifiers )
    (type int)
    (name x)]
  [MethodDeclaration
    (modifiers public )
    (returns void)
    (name work)
    (parameters)
    [BlockStatement
      [ExpressionStatement
        [SIRPushExpression
          (arg
            [PostfixExpression
              (oper 22)
              [FieldExpression
                (left
                  [ThisExpression])
                (name x)]])]]]]
  [MethodDeclaration
    (modifiers public )
    (returns void)
    (name init)
    (parameters)
    [BlockStatement
      [ExpressionStatement
        [AssignmentExpression
          [FieldExpression
            (left
              [ThisExpression])
            (name x)]
          [IntLiteral 0]]]]]
  (peek 0)
  (Pop 0)
  (push 1)
  (OutputType int)]
[Pipeline
  (Parent)
  [MethodDeclaration
    (modifiers public )
    (returns void)
    (name init)
    (parameters)
    [BlockStatement
      [SIRInitStatement
        (args)]
      [SIRInitStatement
        (args)]]]]
[Filter
  (Parent)
  [MethodDeclaration
    (modifiers public )
    (returns void)
    (name work)
    (parameters)
    [BlockStatement
      [ExpressionStatement
        [SIRPushExpression
          (arg
            [BinaryExpression -
              [SIRPopExpression]
              [SIRPopExpression]])]]]]
  [MethodDeclaration
    (modifiers public )
    (returns void)
    (name init)
    (parameters)
    [BlockStatement]]
  (peek 2)
  (Pop 2)
  (push 1)
  (InputType int)
  (OutputType int)]
[Filter
  (Parent)
  [MethodDeclaration
    (modifiers public )
    (returns void)
    (name work)
    (parameters)
    [BlockStatement
      [ExpressionStatement
        [SIRPushExpression
          (arg
            [BinaryExpression +
              [SIRPopExpression]
              [SIRPopExpression]])]]]]
  [MethodDeclaration
    (modifiers public )
    (returns void)
    (name init)
    (parameters)
    [BlockStatement]]
  (peek 2)
  (Pop 2)
  (push 1)
  (InputType int)
  (OutputType int)]
[Filter
  (Parent)
  [MethodDeclaration
    (modifiers public )
    (returns void)
    (name work)
    (parameters)
    [BlockStatement
      [SIRPrintStatement
        (arg
          [SIRPopExpression])]]]
  [MethodDeclaration
    (modifiers public )
    (returns void)
    (name init)
    (parameters)
    [BlockStatement]]
  (peek 1)
  (Pop 1)
  (push 0)
  (InputType int)]

[ClassDeclaration
  (modifiers public )
  (class Main)
  [ClassBody
    [ClassDeclaration
      (modifiers public )
      (class SIRFilter_1)
      (extends java.lang.Object)
      [ClassBody
        [FieldDeclaration
          (modifiers public )
          (type stream_context*)
          (name context)]
        [FieldDeclaration
          (modifiers public )
          (type tape)
          (name inTape)]
        [FieldDeclaration
          (modifiers public )
          (type tape)
          (name outTape)]
        [FieldDeclaration
          (modifiers )
          (type int)
          (name x)]]]
    [ClassDeclaration
      (modifiers public )
      (class SIRFilter_2_1)
      (extends java.lang.Object)
      [ClassBody
        [FieldDeclaration
          (modifiers public )
          (type stream_context*)
          (name context)]
        [FieldDeclaration
          (modifiers public )
          (type tape)
          (name inTape)]
        [FieldDeclaration
          (modifiers public )
          (type tape)
          (name outTape)]]]
    [ClassDeclaration
      (modifiers public )
      (class SIRFilter_2_2)
      (extends java.lang.Object)
      [ClassBody
        [FieldDeclaration
          (modifiers public )
          (type stream_context*)
          (name context)]
        [FieldDeclaration
          (modifiers public )
          (type tape)
          (name inTape)]
        [FieldDeclaration
          (modifiers public )
          (type tape)
          (name outTape)]]]
    [ClassDeclaration
      (modifiers public )
      (class SIRPipeline_2)
      (extends java.lang.Object)
      [ClassBody
        [FieldDeclaration
          (modifiers public )
          (type stream_context*)
          (name context)]
        [FieldDeclaration
          (modifiers public )
          (type tape)
          (name inTape)]
        [FieldDeclaration
          (modifiers public )
          (type tape)
          (name outTape)]
        [FieldDeclaration
          (modifiers public )
          (type SIRFilter_2_1)
          (name stream1)]
        [FieldDeclaration
          (modifiers public )
          (type SIRFilter_2_2)
          (name stream2)]]]
    [ClassDeclaration
      (modifiers public )
      (class SIRFilter_3)
      (extends java.lang.Object)
      [ClassBody
        [FieldDeclaration
          (modifiers public )
          (type stream_context*)
          (name context)]
        [FieldDeclaration
          (modifiers public )
          (type tape)
          (name inTape)]
        [FieldDeclaration
          (modifiers public )
          (type tape)
          (name outTape)]]]
    [ClassDeclaration
      (modifiers public )
      (class SIRPipeline)
      (extends java.lang.Object)
      [ClassBody
        [FieldDeclaration
          (modifiers public )
          (type stream_context*)
          (name context)]
        [FieldDeclaration
          (modifiers public )
          (type tape)
          (name inTape)]
        [FieldDeclaration
          (modifiers public )
          (type tape)
          (name outTape)]
        [FieldDeclaration
          (modifiers public )
          (type SIRFilter_1)
          (name stream1)]
        [FieldDeclaration
          (modifiers public )
          (type SIRPipeline_2)
          (name stream2)]
        [FieldDeclaration
          (modifiers public )
          (type SIRFilter_3)
          (name stream3)]]]
    [MethodDeclaration
      (modifiers public )
      (returns void)
      (name main)
      (parameters)
      [BlockStatement
        [LIRMainFunction
          (typeName SIRPipeline)
          (init
            [LIRFunctionPointer
              (name SIRPipeline_init)])]]]
    [MethodDeclaration
      (modifiers public )
      (returns void)
      (name hierarchical_work_2)
      (parameters
        [FormalParameter
          (type SIRPipeline)
          (name data)])
      [BlockStatement
        [ExpressionStatement
          [MethodCallExpression
            (name SIRFilter_1_work)
            (args
              [FieldExpression
                (left
                  [NameExpression
                    (name data)])
                (name stream1)])]]
        [ExpressionStatement
          [MethodCallExpression
            (name SIRFilter_1_work)
            (args
              [FieldExpression
                (left
                  [NameExpression
                    (name data)])
                (name stream1)])]]
        [ExpressionStatement
          [MethodCallExpression
            (name SIRFilter_1_work)
            (args
              [FieldExpression
                (left
                  [NameExpression
                    (name data)])
                (name stream1)])]]
        [ExpressionStatement
          [MethodCallExpression
            (name SIRFilter_1_work)
            (args
              [FieldExpression
                (left
                  [NameExpression
                    (name data)])
                (name stream1)])]]
        [ExpressionStatement
          [MethodCallExpression
            (name hierarchical_work_1)
            (args
              [NameExpression
                (name data)])]]
        [ExpressionStatement
          [MethodCallExpression
            (name SIRFilter_3_work)
            (args
              [FieldExpression
                (left
                  [NameExpression
                    (name data)])
                (name stream3)])]]]]
    [MethodDeclaration
      (modifiers public )
      (returns void)
      (name hierarchical_work_1)
      (parameters
        [FormalParameter
          (type SIRPipeline)
          (name data)])
      [BlockStatement
        [ExpressionStatement
          [MethodCallExpression
            (name SIRFilter_2_1_work)
            (args
              [FieldExpression
                (left
                  [NameExpression
                    (name data)])
                (name stream1)])]]
        [ExpressionStatement
          [MethodCallExpression
            (name SIRFilter_2_1_work)
            (args
              [FieldExpression
                (left
                  [NameExpression
                    (name data)])
                (name stream1)])]]
        [ExpressionStatement
          [MethodCallExpression
            (name SIRFilter_2_2_work)
            (args
              [FieldExpression
                (left
                  [NameExpression
                    (name data)])
                (name stream2)])]]]]
    [MethodDeclaration
      (modifiers public )
      (returns void)
      (name SIRFilter_1_work)
      (parameters
        [FormalParameter
          (type SIRFilter_1)
          (name data)])
      [BlockStatement
        [ExpressionStatement
          [SIRPushExpression
            (arg
              [PostfixExpression
                (oper 22)
                [FieldExpression
                  (left
                    [NameExpression
                      (name data)])
                  (name x)]])]]]]
    [MethodDeclaration
      (modifiers public )
      (returns void)
      (name SIRFilter_1_init)
      (parameters
        [FormalParameter
          (type SIRFilter_1)
          (name data)])
      [BlockStatement
        [ExpressionStatement
          [AssignmentExpression
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name x)]
            [IntLiteral 0]]]
        [LIRSetStreamType
          (streamContext
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name context)])
          (stream_type FILTER)]
        [LIRSetPush
          (streamContext
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name context)])
          (push_count 1)]
        [LIRSetPeek
          (streamContext
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name context)])
          (peek_count 1)]
        [LIRSetPop
          (streamContext
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name context)])
          (pop_count 1)]
        [LIRSetWork
          (streamContext
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name context)])
          (work_function
            [LIRFunctionPointer
              (name SIRFilter_1_work)])]]]
    [MethodDeclaration
      (modifiers public )
      (returns void)
      (name SIRFilter_2_1_work)
      (parameters
        [FormalParameter
          (type SIRFilter_2_1)
          (name data)])
      [BlockStatement
        [ExpressionStatement
          [SIRPushExpression
            (arg
              [BinaryExpression -
                [SIRPopExpression]
                [SIRPopExpression]])]]]]
    [MethodDeclaration
      (modifiers public )
      (returns void)
      (name SIRFilter_2_1_init)
      (parameters
        [FormalParameter
          (type SIRFilter_2_1)
          (name data)])
      [BlockStatement
        [LIRSetStreamType
          (streamContext
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name context)])
          (stream_type FILTER)]
        [LIRSetPush
          (streamContext
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name context)])
          (push_count 1)]
        [LIRSetPeek
          (streamContext
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name context)])
          (peek_count 1)]
        [LIRSetPop
          (streamContext
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name context)])
          (pop_count 1)]
        [LIRSetWork
          (streamContext
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name context)])
          (work_function
            [LIRFunctionPointer
              (name SIRFilter_2_1_work)])]]]
    [MethodDeclaration
      (modifiers public )
      (returns void)
      (name SIRFilter_2_2_work)
      (parameters
        [FormalParameter
          (type SIRFilter_2_2)
          (name data)])
      [BlockStatement
        [ExpressionStatement
          [SIRPushExpression
            (arg
              [BinaryExpression +
                [SIRPopExpression]
                [SIRPopExpression]])]]]]
    [MethodDeclaration
      (modifiers public )
      (returns void)
      (name SIRFilter_2_2_init)
      (parameters
        [FormalParameter
          (type SIRFilter_2_2)
          (name data)])
      [BlockStatement
        [LIRSetStreamType
          (streamContext
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name context)])
          (stream_type FILTER)]
        [LIRSetPush
          (streamContext
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name context)])
          (push_count 1)]
        [LIRSetPeek
          (streamContext
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name context)])
          (peek_count 1)]
        [LIRSetPop
          (streamContext
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name context)])
          (pop_count 1)]
        [LIRSetWork
          (streamContext
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name context)])
          (work_function
            [LIRFunctionPointer
              (name SIRFilter_2_2_work)])]]]
    [MethodDeclaration
      (modifiers public )
      (returns void)
      (name SIRPipeline_2_init)
      (parameters
        [FormalParameter
          (type SIRPipeline_2)
          (name data)])
      [BlockStatement
        [LIRSetStreamType
          (streamContext
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name context)])
          (stream_type PIPELINE)]
        [LIRSetChild
          (parentContext
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name context)])
          (childType SIRFilter_2_1)
          (childName stream1)]
        [LIRSetChild
          (parentContext
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name context)])
          (childType SIRFilter_2_2)
          (childName stream2)]
        [ExpressionStatement
          [MethodCallExpression
            (name SIRFilter_2_1_init)
            (args
              [FieldExpression
                (left
                  [NameExpression
                    (name data)])
                (name stream1)])]]
        [ExpressionStatement
          [MethodCallExpression
            (name SIRFilter_2_2_init)
            (args
              [FieldExpression
                (left
                  [NameExpression
                    (name data)])
                (name stream2)])]]
        [LIRSetTape
          (streamContext
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name context)])
          (srcStruct
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name stream1)])
          (dstStruct
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name stream2)])
          (type int)
          (size 2)]]]
    [MethodDeclaration
      (modifiers public )
      (returns void)
      (name SIRFilter_3_work)
      (parameters
        [FormalParameter
          (type SIRFilter_3)
          (name data)])
      [BlockStatement
        [SIRPrintStatement
          (arg
            [SIRPopExpression])]]]
    [MethodDeclaration
      (modifiers public )
      (returns void)
      (name SIRFilter_3_init)
      (parameters
        [FormalParameter
          (type SIRFilter_3)
          (name data)])
      [BlockStatement
        [LIRSetStreamType
          (streamContext
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name context)])
          (stream_type FILTER)]
        [LIRSetPush
          (streamContext
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name context)])
          (push_count 0)]
        [LIRSetPeek
          (streamContext
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name context)])
          (peek_count 0)]
        [LIRSetPop
          (streamContext
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name context)])
          (pop_count 0)]
        [LIRSetWork
          (streamContext
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name context)])
          (work_function
            [LIRFunctionPointer
              (name SIRFilter_3_work)])]]]
    [MethodDeclaration
      (modifiers public )
      (returns void)
      (name SIRPipeline_init)
      (parameters
        [FormalParameter
          (type SIRPipeline)
          (name data)])
      [BlockStatement
        [LIRSetStreamType
          (streamContext
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name context)])
          (stream_type PIPELINE)]
        [LIRSetChild
          (parentContext
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name context)])
          (childType SIRFilter_1)
          (childName stream1)]
        [LIRSetChild
          (parentContext
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name context)])
          (childType SIRPipeline_2)
          (childName stream2)]
        [LIRSetChild
          (parentContext
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name context)])
          (childType SIRFilter_3)
          (childName stream3)]
        [ExpressionStatement
          [MethodCallExpression
            (name SIRFilter_1_init)
            (args
              [FieldExpression
                (left
                  [NameExpression
                    (name data)])
                (name stream1)])]]
        [ExpressionStatement
          [MethodCallExpression
            (name SIRPipeline_2_init)
            (args
              [FieldExpression
                (left
                  [NameExpression
                    (name data)])
                (name stream2)])]]
        [ExpressionStatement
          [MethodCallExpression
            (name SIRFilter_3_init)
            (args
              [FieldExpression
                (left
                  [NameExpression
                    (name data)])
                (name stream3)])]]
        [LIRSetTape
          (streamContext
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name context)])
          (srcStruct
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name stream1)])
          (dstStruct
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name stream2)])
          (type int)
          (size 4)]
        [LIRSetTape
          (streamContext
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name context)])
          (srcStruct
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name stream2)])
          (dstStruct
            [FieldExpression
              (left
                [NameExpression
                  (name data)])
              (name stream3)])
          (type int)
          (size 1)]]]]]
*/

#include "streamit.h"

#include <stdio.h>

#include <stdlib.h>


typedef struct SIRFilter_1 {
  stream_context* context;
  tape inTape;
  tape outTape;
  int x;
} _SIRFilter_1, *SIRFilter_1;
typedef struct SIRFilter_2_1 {
  stream_context* context;
  tape inTape;
  tape outTape;
} _SIRFilter_2_1, *SIRFilter_2_1;
typedef struct SIRFilter_2_2 {
  stream_context* context;
  tape inTape;
  tape outTape;
} _SIRFilter_2_2, *SIRFilter_2_2;
typedef struct SIRPipeline_2 {
  stream_context* context;
  tape inTape;
  tape outTape;
  SIRFilter_2_1 stream1;
  SIRFilter_2_2 stream2;
} _SIRPipeline_2, *SIRPipeline_2;
typedef struct SIRFilter_3 {
  stream_context* context;
  tape inTape;
  tape outTape;
} _SIRFilter_3, *SIRFilter_3;
typedef struct SIRPipeline {
  stream_context* context;
  tape inTape;
  tape outTape;
  SIRFilter_1 stream1;
  SIRPipeline_2 stream2;
  SIRFilter_3 stream3;
} _SIRPipeline, *SIRPipeline;
typedef struct Main {
} _Main, *Main;
void main();
void hierarchical_work_2(SIRPipeline data);
void hierarchical_work_1(SIRPipeline data);
void SIRFilter_1_work(SIRFilter_1 data);
void SIRFilter_1_init(SIRFilter_1 data);
void SIRFilter_2_1_work(SIRFilter_2_1 data);
void SIRFilter_2_1_init(SIRFilter_2_1 data);
void SIRFilter_2_2_work(SIRFilter_2_2 data);
void SIRFilter_2_2_init(SIRFilter_2_2 data);
void SIRPipeline_2_init(SIRPipeline_2 data);
void SIRFilter_3_work(SIRFilter_3 data);
void SIRFilter_3_init(SIRFilter_3 data);
void SIRPipeline_init(SIRPipeline data);
void main() {
  SIRPipeline s = malloc(sizeof(_SIRPipeline));
  s->context = create_context(s);
  SIRPipeline_init(s);
  streamit_run(s->context);
}

void hierarchical_work_2(SIRPipeline data) {
  SIRFilter_1_work(data->stream1);
  SIRFilter_1_work(data->stream1);
  hierarchical_work_1(data);
  SIRFilter_3_work(data->stream3);
  SIRFilter_3_work(data->stream3);
}

void hierarchical_work_1(SIRPipeline data) {
  run_splitter(data->stream2->context);
  run_splitter(data->stream2->context);
  SIRFilter_2_1_work(data->stream2->stream1);
  SIRFilter_2_2_work(data->stream2->stream2);
  run_joiner(data->stream2->context);
}

void SIRFilter_1_work(SIRFilter_1 data) {
  PUSH(data->context, int, data->x++);
}

void SIRFilter_1_init(SIRFilter_1 data) {
  data->x = 0;
  set_stream_type(data->context, FILTER);
  set_push(data->context, 1);
  set_peek(data->context, 1);
  set_pop(data->context, 1);
  set_work(data->context, (work_fn)SIRFilter_1_work);
}

void SIRFilter_2_1_work(SIRFilter_2_1 data) {
  PUSH(data->context, int, POP(data->context, int) - POP(data->context, int));
}

void SIRFilter_2_1_init(SIRFilter_2_1 data) {
  set_stream_type(data->context, FILTER);
  set_push(data->context, 1);
  set_peek(data->context, 1);
  set_pop(data->context, 1);
  set_work(data->context, (work_fn)SIRFilter_2_1_work);
}

void SIRFilter_2_2_work(SIRFilter_2_2 data) {
  PUSH(data->context, int, POP(data->context, int) + POP(data->context, int));
}

void SIRFilter_2_2_init(SIRFilter_2_2 data) {
  set_stream_type(data->context, FILTER);
  set_push(data->context, 1);
  set_peek(data->context, 1);
  set_pop(data->context, 1);
  set_work(data->context, (work_fn)SIRFilter_2_2_work);
}

void SIRPipeline_2_init(SIRPipeline_2 data) {
  set_stream_type(data->context, SPLIT_JOIN);
  data->stream1 = malloc(sizeof(_SIRFilter_2_1));
  data->stream1->context = create_context(data->stream1);
  register_child(data->context, data->stream1->context);
  data->stream2 = malloc(sizeof(_SIRFilter_2_2));
  data->stream2->context = create_context(data->stream2);
  register_child(data->context, data->stream2->context);
  SIRFilter_2_1_init(data->stream1);
  SIRFilter_2_2_init(data->stream2);
  set_splitter(data->context, DUPLICATE, 2);
  set_joiner(data->context, ROUND_ROBIN, 2, 1, 1);
  create_split_tape(data->context, 0, data->stream1->context, sizeof(int), 2);
  create_split_tape(data->context, 1, data->stream2->context, sizeof(int), 2);
  create_join_tape(data->stream1->context, data->context, 0, sizeof(int), 1);
  create_join_tape(data->stream2->context, data->context, 1, sizeof(int), 1);  
}

void SIRFilter_3_work(SIRFilter_3 data) {
  printf("%d\n", POP(data->context, int));
}

void SIRFilter_3_init(SIRFilter_3 data) {
  set_stream_type(data->context, FILTER);
  set_push(data->context, 0);
  set_peek(data->context, 0);
  set_pop(data->context, 0);
  set_work(data->context, (work_fn)SIRFilter_3_work);
}

void SIRPipeline_init(SIRPipeline data) {
  set_stream_type(data->context, PIPELINE);
  data->stream1 = malloc(sizeof(_SIRFilter_1));
  data->stream1->context = create_context(data->stream1);
  register_child(data->context, data->stream1->context);
  data->stream2 = malloc(sizeof(_SIRPipeline_2));
  data->stream2->context = create_context(data->stream2);
  register_child(data->context, data->stream2->context);
  data->stream3 = malloc(sizeof(_SIRFilter_3));
  data->stream3->context = create_context(data->stream3);
  register_child(data->context, data->stream3->context);
  SIRFilter_1_init(data->stream1);
  SIRPipeline_2_init(data->stream2);
  SIRFilter_3_init(data->stream3);
  create_tape(data->stream1->context, data->stream2->context, sizeof(int), 4);
  create_tape(data->stream2->context, data->stream3->context, sizeof(int), 2);
  set_work(data->context, (work_fn)hierarchical_work_2);
}

