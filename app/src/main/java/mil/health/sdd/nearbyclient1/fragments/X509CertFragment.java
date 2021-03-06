package mil.health.sdd.nearbyclient1.fragments;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import mil.health.sdd.nearbyclient1.CertInfo;
import mil.health.sdd.nearbyclient1.R;


/**
 * A simple {@link Fragment} subclass.
 */
public class X509CertFragment extends Fragment {
    CertificateListener mCallback;
    CertInfo certInfo;
    private String title = "";

    public X509CertFragment() {

    }
@Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (CertificateListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement CertificateListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_x509_cert, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // Set values for view here
        TextView tTitle = (TextView) view.findViewById(R.id.textViewFragmentCertTitle);
        TextView tCN = (TextView) view.findViewById(R.id.textViewFragmentCN);
        TextView tCountry = (TextView) view.findViewById(R.id.textViewFragmentCountry);
        TextView tState = (TextView) view.findViewById(R.id.textViewFragmentState);
        TextView tLocality = (TextView) view.findViewById(R.id.textViewFragmentLocality);
        TextView tOrganization = (TextView) view.findViewById(R.id.textViewFragmentOrganization);



        Button bDelete = (Button) view.findViewById(R.id.buttonFragmentDelete);

        // update view

        tTitle.setText(this.title);

        if(certInfo == null){
            tCN.setText("Empty Cert");
        } else {
            tCN.setText(certInfo.getCn());
            tCountry.setText(certInfo.getCountry());
            tState.setText(certInfo.getState());
            tLocality.setText(certInfo.getLocality());
            tOrganization.setText(certInfo.getOrganization());
        }
        bDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               mCallback.onClickDelete();
            }
        });
    }

    public void setCert(CertInfo certInfo){
        this.certInfo = certInfo;
    }
    public void setTitle(String title){
        this.title = title;
    }

    public interface CertificateListener {
        public void onClickDelete();
    }

}
