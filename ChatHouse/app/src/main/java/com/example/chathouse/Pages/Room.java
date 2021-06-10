package com.example.chathouse.Pages;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.LinearLayoutCompat;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.widget.LinearLayout;
import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.example.chathouse.API.ChatHouseAPI;
import com.example.chathouse.R;
import com.example.chathouse.Utility.Constants;
import com.example.chathouse.ViewModels.Acount.ProfileInformation;
import com.example.chathouse.ViewModels.Acount.SearchPerson;
import com.example.chathouse.ViewModels.Chat.LoadAllMessagesViewModel;
import com.example.chathouse.ViewModels.Chat.ReceiveRoomNotification;
import com.example.chathouse.ViewModels.Room.ChatBoxModel;
import com.example.chathouse.ViewModels.Room.GetRoomViewModel;
import com.example.chathouse.ViewModels.Chat.JoinRoomViewModel;
import com.example.chathouse.ViewModels.Chat.MessageViewModel;
import com.example.chathouse.ViewModels.Search.InputRoomSearchViewModel;
import com.example.chathouse.ViewModels.Search.InputSearchViewModel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import microsoft.aspnet.signalr.client.hubs.HubProxy;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;

public class Room extends FragmentActivity {
    private TextView RoomName;
    private ImageButton Leave;
    private GetRoomViewModel RoomInfo;
    private String Creator;
    private String Name;
    HubConnection hubConnection; //Do the signalR definitions
    JoinRoomViewModel JoinHub = new JoinRoomViewModel();
    int RoomId;
    String Token;
    MessageViewModel Message = new MessageViewModel();
    private TextView Send;
    private EditText MessageText;
    SearchPerson me;
    ArrayList<LoadAllMessagesViewModel> LoadMessages = new ArrayList<>();
    ProfileInformation Response;
    Gson gson;
    public ChatBoxAdaptor ChatAdaptor;
    public ArrayList<ChatBoxModel> Chats = new ArrayList<ChatBoxModel>();
    public ListView chatBoxListView;
    public ImageButton downBtn;
    public int isReplying = -1;


    public ListView getChatBoxListView() {
        return chatBoxListView;
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);
        Bundle bundle = getIntent().getExtras();

        RoomName = (TextView) findViewById(R.id.RoomName);
        Send = (TextView) findViewById(R.id.SendButton);
        MessageText = (EditText) findViewById(R.id.Message);
        chatBoxListView = (ListView) findViewById(R.id.chatBox);
        downBtn = (ImageButton) findViewById(R.id.chat_downBtn);
        Leave = (ImageButton) findViewById(R.id.LeaveRoom);

        SharedPreferences settings = getSharedPreferences("Storage", MODE_PRIVATE);
        Token = settings.getString("Token", "n/a");
        String Username = settings.getString("Username", "n/a");

        RoomId = bundle.getInt("RoomId");


//        ChatBoxModel test1 = new ChatBoxModel();
//        test1.setId(1);
//        test1.setMessage("message is here \n my name is ali \n do you know me ?");
//        test1.setFirstName("ali");
//        test1.setLastName("farahat");
//        test1.setTime(new Date());
//        test1.setMode(-1);
//        Chats.add(test1);
//        ChatBoxModel test2 = new ChatBoxModel();
//        test2.setId(2);
//        test2.setParentId(1);
//        test2.setMessage("message 2");
//        test2.setFirstName("melika");
//        test2.setLastName("ahmadi");
//        test2.setTime(new Date());
//        test2.setMode(2);
//        Chats.add(test2);

        ChatAdaptor = new ChatBoxAdaptor(Room.this, Chats, chatBoxListView);
        chatBoxListView.setAdapter(ChatAdaptor);
        ChatAdaptor.notifyDataSetChanged();

        chatBoxListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if ((chatBoxListView.getAdapter().getCount() - 1) - chatBoxListView.getLastVisiblePosition() > 10) {
                    downBtn.setVisibility(View.VISIBLE);
                } else {
                    downBtn.setVisibility(View.GONE);
                }
            }
        });

        downBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chatBoxListView.smoothScrollToPosition(Chats.size() - 1);
            }
        });

        // Hub Join
        Connect();
        DefineMethods();

        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request newRequest = chain.request().newBuilder()
                        .addHeader("Authorization", Token)
                        .build();
                return chain.proceed(newRequest);
            }
        }).build();

        gson = new GsonBuilder()
                .setLenient()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(HttpUrl.get(Constants.baseURL))
                .client(client)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory((GsonConverterFactory.create(gson)))
                .build();


        ChatHouseAPI APIS = retrofit.create(ChatHouseAPI.class);

        Call<ProfileInformation> GetProfile = APIS.GetProfile(Username);
        Call<Void> LeaveRoom = APIS.LeaveRoom(RoomId);
        GetProfile.enqueue(new Callback<ProfileInformation>() {
            @Override
            public void onResponse(Call<ProfileInformation> call, Response<ProfileInformation> response) {
                if (!response.isSuccessful()) {
                    try {
                        System.out.println("1" + response.errorBody().string());
                        System.out.println("1" + response.code());
                    } catch (IOException e) {
                        System.out.println("2" + response.errorBody().toString());

                        e.printStackTrace();
                    }

                    return;
                }

                // Set the values from back
                Response = response.body();

                JoinHub.setRoomId(RoomId);
                SearchPerson person = new SearchPerson(Username, Response.getImageLink(), Response.getFirstName(), Response.getLastName());
                JoinHub.setUser(person);
                Join();
                try {
                    TimeUnit.MILLISECONDS.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                LoadAllMessages(Username, RoomId);

            }

            @Override
            public void onFailure(Call<ProfileInformation> call, Throwable t) {
                Toast.makeText(Room.this, "Hi ali failed" + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });


        Call<GetRoomViewModel> GetRoom = APIS.GetRoom(RoomId);
        GetRoom.enqueue(new Callback<GetRoomViewModel>() {
            @Override
            public void onResponse(Call<GetRoomViewModel> call, retrofit2.Response<GetRoomViewModel> response) {
                if (!response.isSuccessful()) {
                    try {
                        System.out.println("1" + response.errorBody().string());
                        System.out.println("1" + response.code());
                        System.out.println(response.errorBody().string());
                    } catch (IOException e) {
                        System.out.println("2" + response.errorBody().toString());
                        e.printStackTrace();
                    }
                    return;
                }
                GetRoomViewModel Response = response.body();
                Creator = Response.getCreator().getUsername();
                Name = Response.getName();
                RoomName.setText(Name);

            }

            @Override
            public void onFailure(Call<GetRoomViewModel> call, Throwable t) {
                Toast.makeText(Room.this, t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        RoomName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Room.this, com.example.chathouse.Pages.RoomInfo.class);
                Bundle bundle = new Bundle();
                bundle.putInt("RoomId", RoomId);
                bundle.putString("Creator", Creator);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });

        chatBoxListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ShowFeatures(view, (ChatBoxModel) chatBoxListView.getAdapter().getItem(position));
            }
        });

        Send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!MessageText.getText().toString().equals("")) {
                    Message.setMessage(MessageText.getText().toString().trim());
                    Message.setMe(true);
                    Message.setRoomId(RoomId);

                    if (isReplying != -1)
                        Message.setParentId(isReplying);
                    else
                        Message.setParentId(-1);
                    me = new SearchPerson(Response.getUsername(), Response.getImageLink(), Response.getFirstName(), Response.getLastName());

                    Message.setUserModel(me);
                    MessageText.setText("");
                    SendMessage(Message);
                }

            }
        });

        Leave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LeaveRoom.enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                        if (!response.isSuccessful()) {
                            try {
                                System.out.println("1" + response.errorBody().string());
                                System.out.println("1" + response.code());
                                System.out.println(response.errorBody().string());
                            } catch (IOException e) {
                                System.out.println("2" + response.errorBody().toString());
                                e.printStackTrace();
                            }
                            return;
                        }
                        System.out.println("Deleted");
                        finish();
                        Leave();
                        Intent intent = new Intent(Room.this, AcitivityPage.class);

                        startActivity(intent);
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(Room.this, "Request failed", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

    }

    private void Connect() {

        hubConnection = HubConnectionBuilder.create(Constants.serverUrl).withAccessTokenProvider(Single.defer(() -> {
            return Single.just(Token);
        })).build();

        hubConnection.start();

    }

    public void Leave() {
        try {
            hubConnection.invoke("LeaveRoom", JoinHub);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void Join() {
        try {
            hubConnection.invoke("JoinRoom", JoinHub);


        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void SendMessage(MessageViewModel message) {
        try {
            hubConnection.invoke("SendMessageToRoom", message);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    private void LoadAllMessages(String username, int roomId) {
        try {
            hubConnection.invoke("LoadRoomMessages", roomId, username);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void DefineMethods() {
        hubConnection.on("ReceiveRoomMessage", (messageModel) ->
        {
            ChatBoxModel x = new ChatBoxModel();
            x.setId(messageModel.getRoomId());
            x.setParentId(messageModel.getParentId());
            x.setFirstName(messageModel.getUserModel().getFirstName());
            x.setLastName(messageModel.getUserModel().getLastName());
            x.setMessage(messageModel.getMessage().toString());
            x.setImageLink(messageModel.getUserModel().getImageLink());
            x.setUserName(messageModel.getUserModel().getUsername());
            x.setTime(new Date());
            if (messageModel.isMe() == true) {
//                Toast.makeText(Room.this, "parentId is: "+x.getParentId(), Toast.LENGTH_LONG).show();
                Log.println(Log.ERROR, "ssssssss", "parentId is : " + String.valueOf(messageModel.getParentId()));
                if (x.getParentId() == 0)
                    x.setMode(1);
                else
                    x.setMode(2);
            } else if (messageModel.isMe() == false) {

                if (x.getParentId() == 0)
                    x.setMode(-1);
                else
                    x.setMode(-2);
            }
            runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    Chats.add(x);
                    ChatAdaptor.notifyDataSetChanged();
                    chatBoxListView.smoothScrollToPosition(Chats.size() - 1);
                }
            });

            String text = "";
            int RoomId = messageModel.getRoomId();
            String message = messageModel.getMessage().toString();
            String senderLastName = messageModel.getUserModel().getLastName();
            String senderUsername = messageModel.getUserModel().getUsername();
            String senderFirstName = messageModel.getUserModel().getFirstName();
            Boolean IsMe = messageModel.isMe();
            if (IsMe) {
                text += "Your Message To  Room number #" + RoomId + ":";
            } else {
                text += "New Message From " + senderFirstName + " " + senderLastName + " To  Room number #" + RoomId + ":";
            }

//            Toast.makeText(Room.this, text + message, Toast.LENGTH_LONG).show();
            System.out.println(text + message);
        }, MessageViewModel.class);

        hubConnection.on("ReceiveRoomNotification", (ReceiveRoomNotification) ->
        {
            ChatBoxModel x = new ChatBoxModel();
            x.setUserName(ReceiveRoomNotification.getUserModel().getUsername());

            x.setMode(0);

            if (ReceiveRoomNotification.getNotification() == 0) {
                if (ReceiveRoomNotification.getMe()) {
                    x.setMessage("You joined this room");

                } else {
                    x.setMessage(ReceiveRoomNotification.getUserModel().getUsername() + " join this room");
                }
            } else {
                if (ReceiveRoomNotification.getMe()) {
                    x.setMessage("You left this room");
                } else {
                    x.setMessage(ReceiveRoomNotification.getUserModel().getUsername() + " left this room");
                }
            }
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    Chats.add(x);
                    ChatAdaptor.notifyDataSetChanged();
                    chatBoxListView.smoothScrollToPosition(Chats.size() - 1);
                }
            });
        }, ReceiveRoomNotification.class);

        hubConnection.on("ReceiveRoomAllMessages", (messageModel) ->
        {
            for (Object X : messageModel) {

                String s2 = gson.toJson(X);
                LoadAllMessagesViewModel x = gson.fromJson(s2, LoadAllMessagesViewModel.class);
                ChatBoxModel chat = new ChatBoxModel();
                int contentType = x.contetntType;
                if (contentType == 0) {
                    chat.setId(x.getId());
                    chat.setParentId(x.getParentId());
                    chat.setFirstName(x.getSender().getFirstName());
                    chat.setLastName(x.getSender().getLastName());
                    chat.setImageLink(x.getSender().getImageLink());
                    chat.setUserName(x.getSender().getUsername());
                    chat.setTime(x.sentDate);
                    if (x.getMe() == true) {
                        if (x.getParentId() == -1)
                            chat.setMode(1);
                        else
                            chat.setMode(2);
                    } else if (x.getMe() == false) {
                        if (x.getParentId() == -1)
                            chat.setMode(-1);
                        else
                            chat.setMode(-2);
                    }
                    chat.setMessage(x.getContent());
                } else if (contentType == 2) {
                    chat.setMode(0);
                    if (x.getMe()) {
                        chat.setMessage("You joined this room");

                    } else {
                        chat.setMessage(x.getSender().getUsername() + " join this room");
                    }
                } else if (contentType == 3) {
                    chat.setMode(0);
                    if (x.getMe()) {
                        chat.setMessage("You left this room");

                    } else {
                        chat.setMessage(x.getSender().getUsername() + " left this room");
                    }
                }
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        Chats.add(chat);
                        ChatAdaptor.notifyDataSetChanged();
                        chatBoxListView.smoothScrollToPosition(Chats.size() - 1);
                    }
                });
            }
        }, (Class<List<LoadAllMessagesViewModel>>) (Object) List.class);
    }

    @SuppressLint("RestrictedApi")
    public void ShowFeatures(View v, ChatBoxModel chat) {
        MenuBuilder chatFeatures = new MenuBuilder(this);

        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.chat_features, chatFeatures);
        MenuPopupHelper optionsMenu = new MenuPopupHelper(this, chatFeatures, v);
        optionsMenu.setForceShowIcon(true);
        optionsMenu.setGravity(1);
        chatFeatures.setCallback(new MenuBuilder.Callback() {
            @Override
            public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.reply:
                        isReplying = chat.getId();
                        break;
                }
                return true;
            }

            @Override
            public void onMenuModeChange(MenuBuilder menu) {}
        });
        optionsMenu.show(300,-150);
    }
}


class ChatBoxAdaptor extends BaseAdapter {

    Context mContext;
    LayoutInflater inflater;
    ListView ChatListView;
    private List<ChatBoxModel> ChatsList = null;

    public ChatBoxAdaptor(Context context, List<ChatBoxModel> chatsList, ListView chatListView) {
        mContext = context;
        this.ChatsList = chatsList;
        inflater = LayoutInflater.from(mContext);
        this.ChatListView = chatListView;

    }

    public class ViewHolder {
        TextView message;
        TextView name;
        TextView time;
        ImageView Image;

        TextView replied_message;
        TextView replied_name;

        LinearLayout repliedPart;
    }

    @Override
    public int getCount() {
        return ChatsList.size();
    }

    @Override
    public ChatBoxModel getItem(int position) {
        return ChatsList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public View getView(final int position, View view, ViewGroup parent) {
        final ViewHolder holder;
        holder = new ViewHolder();
        ChatBoxModel chat = ChatsList.get(position);
        if (ChatsList.get(position).getMode() == -1) {  //left
            view = inflater.inflate(R.layout.chat_left, null);

            holder.message = (TextView) view.findViewById(R.id.chat_message_left);
            holder.name = (TextView) view.findViewById(R.id.chat_name_left);
            holder.time = (TextView) view.findViewById(R.id.chat_time_left);
            holder.Image = (ImageView) view.findViewById(R.id.chat_image_left);
            view.setTag(holder);

            holder.message.setText(chat.getMessage());
            holder.name.setText(chat.getFirstName() + " " + chat.getLastName());

            SimpleDateFormat formatter = new SimpleDateFormat("hh:mm aa");
            holder.time.setText(formatter.format(chat.getTime()));

            RequestOptions options = new RequestOptions()
                    .placeholder(R.mipmap.default_user_profile)
                    .centerCrop();
            Glide.with(mContext).load(chat.getImageLink())
                    .apply(options).transform(new CircleCrop()).into(holder.Image);

            holder.Image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mContext, ProfilePage.class);
                    Bundle bundle1 = new Bundle();
                    bundle1.putString("Username", chat.getUserName());
                    intent.putExtras(bundle1);
                    mContext.startActivity(intent);
                }
            });


        } else if (ChatsList.get(position).getMode() == 1) {
            view = inflater.inflate(R.layout.chat_right, null);

            holder.message = (TextView) view.findViewById(R.id.chat_message_right);
            holder.time = (TextView) view.findViewById(R.id.chat_time_right);
            view.setTag(holder);

            holder.message.setText(chat.getMessage());

            SimpleDateFormat formatter = new SimpleDateFormat("hh:mm aa");
            holder.time.setText(formatter.format(chat.getTime()));


        } else if (ChatsList.get(position).getMode() == 0) {
            view = inflater.inflate(R.layout.chat_middle, null);

            holder.message = (TextView) view.findViewById(R.id.chat_middle);
            view.setTag(holder);

            holder.message.setText(chat.getMessage());

        } else if (ChatsList.get(position).getMode() == -2) {
            view = inflater.inflate(R.layout.chat_reply_left, null);

            holder.message = (TextView) view.findViewById(R.id.chat_message_left_reply);
            holder.name = (TextView) view.findViewById(R.id.chat_name_left_reply);
            holder.time = (TextView) view.findViewById(R.id.chat_time_left_reply);
            holder.replied_message = (TextView) view.findViewById(R.id.chat_replied_message_left_reply);
            holder.replied_name = (TextView) view.findViewById(R.id.chat_replied_name_left_reply);
            holder.Image = (ImageView) view.findViewById(R.id.chat_image_left_reply);
            holder.repliedPart = (LinearLayout) view.findViewById(R.id.chat_replied_box_left_reply);


            holder.message.setText(chat.getMessage());
            holder.name.setText(chat.getFirstName() + " " + chat.getLastName());

            SimpleDateFormat formatter = new SimpleDateFormat("hh:mm aa");
            holder.time.setText(formatter.format(chat.getTime()));

            RequestOptions options = new RequestOptions()
                    .placeholder(R.mipmap.default_user_profile)
                    .centerCrop();
            Glide.with(mContext).load(chat.getImageLink())
                    .apply(options).transform(new CircleCrop()).into(holder.Image);


            ChatBoxModel replied = null;
            int repliedPosition = -1;
            for (ChatBoxModel ch : ChatsList) {
                repliedPosition += 1;
                if (ch.getId() == chat.getParentId()) {
                    replied = ch;
                    chat.setParentPosition(repliedPosition);
                    break;
                }
            }
            if (replied != null) {
                holder.replied_message.setText(replied.getMessage().replace('\n', ' '));
                holder.replied_name.setText(replied.getFirstName() + " " + replied.getLastName());

                int finalRepliedPosition = repliedPosition;
                holder.repliedPart.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ChatListView.smoothScrollToPosition(finalRepliedPosition);
                    }
                });
            } else
                holder.replied_message.setText("no message found!");


            view.setTag(holder);
        } else if (ChatsList.get(position).getMode() == 2) {
            view = inflater.inflate(R.layout.chat_reply_right, null);

            holder.message = (TextView) view.findViewById(R.id.chat_message_right_reply);
            holder.time = (TextView) view.findViewById(R.id.chat_time_right_reply);
            holder.replied_message = (TextView) view.findViewById(R.id.chat_replied_message_right_reply);
            holder.replied_name = (TextView) view.findViewById(R.id.chat_replied_name_right_reply);
            holder.repliedPart = (LinearLayout) view.findViewById(R.id.chat_replied_box_right_reply);

            holder.message.setText(chat.getMessage());

            SimpleDateFormat formatter = new SimpleDateFormat("hh:mm aa");
            holder.time.setText(formatter.format(chat.getTime()));


            ChatBoxModel replied = null;
            int repliedPosition = -1;
            for (ChatBoxModel ch : ChatsList) {
                repliedPosition += 1;
                if (ch.getId() == chat.getParentId()) {
                    replied = ch;
                    chat.setParentPosition(repliedPosition);
                    break;
                }
            }
            if (replied != null) {
                holder.replied_message.setText(replied.getMessage().replace('\n', ' '));
                holder.replied_name.setText(replied.getFirstName() + " " + replied.getLastName());

                int finalRepliedPosition = repliedPosition;
                holder.repliedPart.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ChatListView.smoothScrollToPosition(finalRepliedPosition);
                    }
                });
            } else
                holder.replied_message.setText("no message found!");

            view.setTag(holder);
        }


        return view;
    }
}

